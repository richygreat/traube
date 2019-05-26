package com.github.richygreat.traube.param.model;

import lombok.Value;

@Value
public class MongoServer {
	private String hostName; // with port
	private int index;
}
