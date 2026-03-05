package com.example.demo;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		configureDatasourceFromEnvironment();
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void configureDatasourceFromEnvironment() {
		String alreadyConfiguredUrl = firstNonBlank(
				System.getProperty("spring.datasource.url"),
				System.getenv("SPRING_DATASOURCE_URL"),
				System.getenv("JDBC_DATABASE_URL")
		);

		if (alreadyConfiguredUrl != null) {
			return;
		}

		String databaseUrl = firstNonBlank(
				System.getenv("DATABASE_URL"),
				System.getenv("INTERNAL_DATABASE_URL")
		);

		if (databaseUrl == null) {
			return;
		}

		if (databaseUrl.startsWith("jdbc:postgresql://")) {
			System.setProperty("spring.datasource.url", databaseUrl);
			return;
		}

		if (!(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
			return;
		}

		URI uri = URI.create(databaseUrl);
		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String database = uri.getPath() == null ? "" : uri.getPath();

		if (host == null || database.isBlank()) {
			return;
		}

		String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + database;
		System.setProperty("spring.datasource.url", jdbcUrl);

		String userInfo = uri.getUserInfo();
		if (userInfo != null && !userInfo.isBlank()) {
			String[] credentials = userInfo.split(":", 2);
			if (credentials.length > 0 && !credentials[0].isBlank()) {
				System.setProperty("spring.datasource.username", decode(credentials[0]));
			}
			if (credentials.length > 1 && !credentials[1].isBlank()) {
				System.setProperty("spring.datasource.password", decode(credentials[1]));
			}
		}
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

}
