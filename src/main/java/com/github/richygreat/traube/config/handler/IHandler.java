package com.github.richygreat.traube.config.handler;

import com.github.richygreat.traube.config.ApplicationType;
import com.github.richygreat.traube.param.model.IParams;

public interface IHandler {
    void handle(IParams params);

    ApplicationType getType();
}
