package com.github.richygreat.traube.config.handler;

import org.springframework.stereotype.Component;

import com.github.richygreat.traube.aws.EC2InfoService;
import com.github.richygreat.traube.config.ApplicationType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MongoShardServerHandler extends MongoReplicaHandler {
	private final EC2InfoService ec2InfoService;

	@Override
	public ApplicationType getType() {
		return ApplicationType.mongoshard;
	}

	@Override
	EC2InfoService getEC2InfoService() {
		return ec2InfoService;
	}
}
