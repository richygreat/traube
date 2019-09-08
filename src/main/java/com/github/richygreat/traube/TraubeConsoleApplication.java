package com.github.richygreat.traube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = MongoAutoConfiguration.class)
public class TraubeConsoleApplication {
    public static void main(String[] args) {
	SpringApplication.run(TraubeConsoleApplication.class, args);
    }
}
