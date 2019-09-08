package com.github.richygreat.traube.param.builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.richygreat.traube.param.model.MongoParams;

public class MongoParamsBuilder implements IParamsBuilder<MongoParams> {
    @Override
    public MongoParams build(Map<String, List<String>> paramMap) {
	MongoParams mongoParams = new MongoParams();
	Optional.ofNullable(paramMap.get("privateip")).ifPresent(p -> mongoParams.setPrivateIp(p.get(0)));
	Optional.ofNullable(paramMap.get("username")).ifPresent(p -> mongoParams.setUsername(p.get(0)));
	Optional.ofNullable(paramMap.get("password")).ifPresent(p -> mongoParams.setPassword(p.get(0)));
	return mongoParams;
    }
}
