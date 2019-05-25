package com.github.richygreat.traube.config.handler;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import com.github.richygreat.traube.config.ApplicationType;
import com.github.richygreat.traube.param.model.IParams;
import com.github.richygreat.traube.param.model.MongoParams;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.internal.connection.ServerAddressHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MongoConfigServerHandler implements IHandler {
	private static final String ADMIN_DB = "admin";

	@Override
	public void handle(IParams params) {
		MongoParams mongoParams = (MongoParams) params;
		String localIp = "54.234.246.116";
		log.info("handle: Entering mongoParams: {} localIp: {}", mongoParams, localIp);

		MongoCredential credential = MongoCredential.createCredential("admin", ADMIN_DB, "password".toCharArray());

		MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(localIp, 27018), credential,
				new MongoClientOptions.Builder().build());

		Document replicaSetStatus = mongoClient.getDatabase(ADMIN_DB).runCommand(new Document("replSetGetStatus", 1));
		log.info("replicaSetStatus: {}", replicaSetStatus);

		Document result = null;
		Bson command = null;
		if (replicaSetStatus != null && Double.valueOf(1.0).equals(replicaSetStatus.getDouble("ok"))) {
			log.info("rs: {}", replicaSetStatus.get("members"));
			@SuppressWarnings("unchecked")
			List<Document> members = (List<Document>) replicaSetStatus.get("members");
			int oldVersion = members.stream().mapToInt(member -> member.getInteger("configVersion")).max().getAsInt();
			log.info("oldVersion: {}", oldVersion);

			List<String> memberIps = members.stream().map(member -> String.valueOf(member.get("name")))
					.collect(Collectors.toList());
			if (!memberIps.contains(localIp + ":27018")) {
				memberIps.add(localIp + ":27018");
			}
			log.info("memberIps: {}", memberIps);

			Document config = new Document("_id", "replconfig01");
			config.put("configsvr", true);
			List<Document> newMembers = IntStream.range(0, memberIps.size()).mapToObj(i -> {
				Document localConfig = new Document("_id", i);
				localConfig.put("host", memberIps.get(i));
				return localConfig;
			}).collect(Collectors.toList());

			config.put("version", ++oldVersion);
			config.put("protocolVersion", 1);
			config.put("members", newMembers);
			command = new Document("replSetReconfig", config);
		} else {
			Document config = new Document("_id", "replconfig01");
			config.put("configsvr", true);
			Document localConfig = new Document("_id", 0);
			localConfig.put("host", localIp + ":27018");
			config.put("members", Collections.singletonList(localConfig));
			command = new Document("replSetInitiate", config);
		}
		result = mongoClient.getDatabase(ADMIN_DB).runCommand(command);
		log.info("command: {} result: {}", command, result);
		mongoClient.close();
	}

	@Override
	public ApplicationType getType() {
		return ApplicationType.mongoconfig;
	}
}
