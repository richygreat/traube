package com.github.richygreat.traube.config.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.github.richygreat.traube.config.ApplicationType;
import com.github.richygreat.traube.param.model.IParams;
import com.github.richygreat.traube.param.model.MongoParams;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoCollection;
import com.mongodb.internal.connection.ServerAddressHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MongoShardHandler implements IHandler {
    private static final String ADMIN_DB = "admin";
    private static final String MONGOS_IP = "18.194.249.240";

    @Override
    public void handle(IParams params) {
	MongoParams mongoParams = (MongoParams) params;
	log.info("handle: Entering mongoParams: {}", mongoParams);
	Optional<Document> optionalShards = findShards(mongoParams);
	if (!optionalShards.isPresent()) {
	    log.info("Replica Not Present");
	} else {
	    @SuppressWarnings("unchecked")
	    List<Document> shards = (List<Document>) optionalShards.get().get("members");
	    log.info("shards: {}", shards);
	    if (CollectionUtils.isEmpty(shards)) {
		addShardToAdminDb(mongoParams);
	    }
	}
    }

    private void addShardToAdminDb(MongoParams mongoParams) {
	MongoCredential credential = MongoCredential.createCredential(mongoParams.getUsername(), ADMIN_DB,
		mongoParams.getPassword().toCharArray());
	try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(MONGOS_IP, 27017),
		credential, new MongoClientOptions.Builder().build())) {
	    List<String> databaseNames = new ArrayList<String>();
	    mongoClient.listDatabaseNames().iterator().forEachRemaining(databaseNames::add);
	    log.info("databaseNames: {}", databaseNames);
//	    mongoClient.getDatabase(ADMIN_DB).createCollection("mongoshard");

	    MongoCollection<Document> mongoShardCollection = mongoClient.getDatabase(ADMIN_DB)
		    .getCollection("mongoshard");

	    mongoShardCollection.find().iterator().forEachRemaining(doc -> log.info("doc: {}", doc));

	    mongoShardCollection.insertOne(new Document("replica", 1));

	    long count = mongoShardCollection.countDocuments();
	    log.info("count: {}", count);
	} catch (Exception e) {
	    log.error("addShardToAdminDb: failure", e);
	}
    }

    private Optional<Document> findShards(MongoParams mongoParams) {
	MongoCredential credential = MongoCredential.createCredential(mongoParams.getUsername(), ADMIN_DB,
		mongoParams.getPassword().toCharArray());
	try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(MONGOS_IP, 27017),
		credential, new MongoClientOptions.Builder().build())) {
	    Document shStatus = mongoClient.getDatabase(ADMIN_DB).runCommand(new Document("listShards", 1));
	    if (shStatus != null && Double.valueOf(1.0).equals(shStatus.getDouble("ok"))) {
		return Optional.of(shStatus);
	    }
	} catch (Exception e) {
	    log.error("findShards: failure", e);
	}
	return Optional.empty();
    }

    @Override
    public ApplicationType getType() {
	return ApplicationType.mongoshard;
    }

}
