package com.github.richygreat.traube.config;

import com.github.richygreat.traube.param.builder.IParamsBuilder;
import com.github.richygreat.traube.param.builder.MongoParamsBuilder;
import com.github.richygreat.traube.param.model.IParams;
import com.github.richygreat.traube.param.model.MongoParams;

public enum ApplicationType {
    mongoshard {
	@Override
	public IParamsBuilder<MongoParams> getBuilder() {
	    return new MongoParamsBuilder();
	}
    };

    public abstract IParamsBuilder<? extends IParams> getBuilder();
}
