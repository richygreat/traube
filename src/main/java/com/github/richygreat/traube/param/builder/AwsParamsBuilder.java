package com.github.richygreat.traube.param.builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.richygreat.traube.param.model.AwsParams;

public class AwsParamsBuilder {
	public void populateAwsParams(AwsParams awsParams, Map<String, List<String>> paramMap) {
		Optional.ofNullable(paramMap.get("accessid")).ifPresent(p -> awsParams.setAccessId(p.get(0)));
		Optional.ofNullable(paramMap.get("secretkey")).ifPresent(p -> awsParams.setSecretKey(p.get(0)));
		Optional.ofNullable(paramMap.get("asgname")).ifPresent(p -> awsParams.setAsgName(p.get(0)));
	}
}
