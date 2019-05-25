package com.github.richygreat.traube.param.builder;

import java.util.List;
import java.util.Map;

import com.github.richygreat.traube.param.model.IParams;

public interface IParamsBuilder<T extends IParams> {
	T build(Map<String, List<String>> paramMap);
}
