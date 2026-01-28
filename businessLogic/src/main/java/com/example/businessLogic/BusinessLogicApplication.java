package com.example.businessLogic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusinessLogicApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusinessLogicApplication.class, args);
	}
}
