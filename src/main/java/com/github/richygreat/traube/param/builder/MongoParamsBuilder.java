package com.github.richygreat.traube.param.builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.richygreat.traube.param.model.MongoParams;

public class MongoParamsBuilder implements IParamsBuilder<MongoParams> {
	@Override
	public MongoParams build(Map<String, List<String>> paramMap) {
		MongoParams mongoParams = new MongoParams();
		Optional.ofNullable(paramMap.get("replica")).ifPresent(p -> mongoParams.setReplicaName(p.get(0)));
		return mongoParams;
	}
}
