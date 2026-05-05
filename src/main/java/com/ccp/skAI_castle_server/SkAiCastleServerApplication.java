package com.ccp.skAI_castle_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkAiCastleServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkAiCastleServerApplication.class, args);
	}

}
