package com.todaydev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TodaydevBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodaydevBackendApplication.class, args);
	}

}
