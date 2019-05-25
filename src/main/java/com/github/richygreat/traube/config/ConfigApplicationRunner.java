package com.github.richygreat.traube.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.github.richygreat.traube.config.handler.IHandler;
import com.github.richygreat.traube.param.model.IParams;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ConfigApplicationRunner implements ApplicationRunner {
	private final EnumMap<ApplicationType, IHandler> handlerMap;

	@Autowired
	public ConfigApplicationRunner(List<IHandler> handlers) {
		handlerMap = handlers.stream().collect(Collectors.toMap(IHandler::getType, Function.identity(), (u, v) -> {
			throw new IllegalStateException(String.format("Duplicate key %s", u));
		}, () -> new EnumMap<>(ApplicationType.class)));
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering run getNonOptionArgs: {} getOptionNames: {}", args.getNonOptionArgs(),
				args.getOptionNames());
		if (CollectionUtils.isEmpty(args.getNonOptionArgs()) || args.getNonOptionArgs().size() > 1
				|| StringUtils.isEmpty(args.getNonOptionArgs().get(0))) {
			throw new RuntimeException("Invalid arguments");
		}
		ApplicationType appType = null;
		try {
			String app = args.getNonOptionArgs().get(0);
			appType = ApplicationType.valueOf(app);
		} catch (IllegalArgumentException e) {
		}
		if (appType == null) {
			throw new RuntimeException("Invalid arguments");
		}
		log.info("appType: {}", appType);
		Map<String, List<String>> paramMap = args.getOptionNames().stream()
				.collect(Collectors.toMap(Function.identity(), k -> args.getOptionValues(k)));
		IParams params = appType.getBuilder().build(paramMap);
		log.info("params: {}", params);
		handlerMap.get(appType).handle(params);
	}
}
