package com.ccp.skAI_castle_server.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        Map<String, Object> properties = new HashMap<>();
        dotenv.entries().forEach(e -> properties.put(e.getKey(), e.getValue()));
        environment.getPropertySources().addFirst(new MapPropertySource("dotenv", properties));
    }
}
