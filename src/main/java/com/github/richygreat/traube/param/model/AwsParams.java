package com.github.richygreat.traube.param.model;

import lombok.Data;

@Data
public abstract class AwsParams implements IParams {
	private String accessId;
	private String secretKey;
	private String asgName;
}
