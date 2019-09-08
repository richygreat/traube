package com.github.richygreat.traube.param.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MongoParams implements IParams {
    private String privateIp;
    private String username;
    private String password;
}
