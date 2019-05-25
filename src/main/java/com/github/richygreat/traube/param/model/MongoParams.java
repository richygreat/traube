package com.github.richygreat.traube.param.model;

import lombok.Data;

@Data
public class MongoParams implements IParams {
	private String replicaName;
	private String username;
	private String password;
}
