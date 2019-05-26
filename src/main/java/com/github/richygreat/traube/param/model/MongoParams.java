package com.github.richygreat.traube.param.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MongoParams extends AwsParams {
	private String replicaName;
	private String username;
	private String password;
}
