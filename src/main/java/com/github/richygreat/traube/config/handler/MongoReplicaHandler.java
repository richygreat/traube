package com.github.richygreat.traube.config.handler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

import com.github.richygreat.traube.aws.EC2InfoService;
import com.github.richygreat.traube.config.ApplicationType;
import com.github.richygreat.traube.param.model.IParams;
import com.github.richygreat.traube.param.model.MongoParams;
import com.github.richygreat.traube.param.model.MongoServer;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.internal.connection.ServerAddressHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MongoReplicaHandler implements IHandler {
	private static final String ADMIN_DB = "admin";
	private static final int CONFIG_SERVER_PORT = 27018;
	private static final int SHARD_SERVER_PORT = 27017;

	@Override
	public void handle(IParams params) {
		MongoParams mongoParams = (MongoParams) params;
		log.info("handle: Entering mongoParams: {}", mongoParams);

		validate(mongoParams);

		handleReplica(mongoParams);
	}

	public void handleReplica(MongoParams mongoParams) {
		int port = ApplicationType.mongoconfig.equals(getType()) ? CONFIG_SERVER_PORT : SHARD_SERVER_PORT;
		String localIp = getEC2InfoService().getCurrentInstancePrivateIp();
		MongoCredential credential = MongoCredential.createCredential(mongoParams.getUsername(), ADMIN_DB,
				mongoParams.getPassword().toCharArray());

		Optional<Document> optionalReplicaSetStatus = findExistingReplica(mongoParams);

		Document result = null;
		Bson command = null;
		MongoClient mongoClient = null;
		if (optionalReplicaSetStatus.isPresent()) {
			@SuppressWarnings("unchecked")
			List<Document> members = (List<Document>) optionalReplicaSetStatus.get().get("members");
			Optional<Document> optionalPrimary = members.stream()
					.filter(member -> Integer.valueOf(0).equals(member.getInteger("_id"))).findFirst();

			log.info("optionalPrimary: {}", optionalPrimary);

			String primaryIp = optionalPrimary.get().getString("name").replace(":" + port, "");
			log.info("primaryIp: {}", primaryIp);

			mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(primaryIp, port), credential,
					new MongoClientOptions.Builder().build());
			command = scaleAsg(mongoParams, port, optionalReplicaSetStatus.get());
		} else {
			mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(localIp, port), credential,
					new MongoClientOptions.Builder().build());
			command = createAsg(mongoParams, port, localIp);
		}
		result = mongoClient.getDatabase(ADMIN_DB).runCommand(command);
		log.info("command: {} result: {}", command, result);
		mongoClient.close();
	}

	private Optional<Document> findExistingReplica(MongoParams mongoParams) {
		int port = ApplicationType.mongoconfig.equals(getType()) ? CONFIG_SERVER_PORT : SHARD_SERVER_PORT;
		List<String> asgIps = getEC2InfoService().getAsgInstances(mongoParams);

		MongoCredential credential = MongoCredential.createCredential(mongoParams.getUsername(), ADMIN_DB,
				mongoParams.getPassword().toCharArray());
		for (String asgIp : asgIps) {
			try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(asgIp, port),
					credential, new MongoClientOptions.Builder().build())) {
				Document rsStatus = mongoClient.getDatabase(ADMIN_DB).runCommand(new Document("replSetGetStatus", 1));
				if (rsStatus != null && Double.valueOf(1.0).equals(rsStatus.getDouble("ok"))) {
					return Optional.of(rsStatus);
				}
			} catch (Exception e) {
			}
		}
		return Optional.empty();
	}

	private Bson createAsg(MongoParams mongoParams, int port, String localIp) {
		Document config = new Document("_id", mongoParams.getReplicaName());
		if (ApplicationType.mongoconfig.equals(getType())) {
			config.put("configsvr", true);
		}
		Document localConfig = new Document("_id", 0);
		localConfig.put("host", localIp + ":" + port);
		config.put("members", Collections.singletonList(localConfig));
		return new Document("replSetInitiate", config);
	}

	private Bson scaleAsg(MongoParams mongoParams, int port, Document replicaSetStatus) {
		@SuppressWarnings("unchecked")
		List<Document> members = (List<Document>) replicaSetStatus.get("members");
		int oldVersion = members.stream().mapToInt(member -> member.getInteger("configVersion")).max().getAsInt();
		log.info("oldVersion: {}", oldVersion);

		List<MongoServer> mongoServers = members.stream()
				.map(member -> new MongoServer(member.getString("name"), member.getInteger("_id")))
				.collect(Collectors.toList());

		List<String> hostNames = mongoServers.stream().map(MongoServer::getHostName).collect(Collectors.toList());
		log.info("existing replica hostNames: {}", hostNames);

		List<String> asgIps = getEC2InfoService().getAsgInstances(mongoParams);
		log.info("asgIps beforeRemoval: {}", asgIps);
		asgIps.removeIf(asgIp -> hostNames.contains(asgIp + ":" + port));
		log.info("asgIps afterRemoval: {}", asgIps);

		asgIps.forEach(asgIp -> mongoServers.add(new MongoServer(asgIp + ":" + port, mongoServers.size())));
		log.info("Added new mongoServers: {}", mongoServers);

		Document config = new Document("_id", mongoParams.getReplicaName());
		if (ApplicationType.mongoconfig.equals(getType())) {
			config.put("configsvr", true);
		}

		List<Document> newMembers = mongoServers.stream().map(server -> {
			Document localConfig = new Document("_id", server.getIndex());
			localConfig.put("host", server.getHostName());
			return localConfig;
		}).collect(Collectors.toList());

		config.put("version", ++oldVersion);
		config.put("protocolVersion", 1);
		config.put("members", newMembers);
		return new Document("replSetReconfig", config);
	}

	public void validate(MongoParams mongoParams) {
		if (StringUtils.isEmpty(mongoParams.getReplicaName()) || StringUtils.isEmpty(mongoParams.getUsername())
				|| StringUtils.isEmpty(mongoParams.getPassword())) {
			throw new RuntimeException("Mandatory params are username, password and replica");
		}
	}

	abstract EC2InfoService getEC2InfoService();
}
