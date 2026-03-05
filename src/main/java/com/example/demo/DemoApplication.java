package com.example.demo;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		configureDatasourceFromEnvironment();
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void configureDatasourceFromEnvironment() {
		String configuredUrl = firstNonBlank(
				System.getProperty("spring.datasource.url"),
				System.getenv("SPRING_DATASOURCE_URL"),
				System.getenv("JDBC_DATABASE_URL"),
				System.getenv("DATABASE_URL"),
				System.getenv("DATABASE_INTERNAL_URL"),
				System.getenv("INTERNAL_DATABASE_URL"),
				System.getenv("POSTGRES_URL"),
				System.getenv("POSTGRES_INTERNAL_URL")
		);

		if (configuredUrl == null) {
			configuredUrl = findDatabaseUrlFromAnyEnv();
		}

		if (configuredUrl != null) {
			String normalizedUrl = normalizeToJdbcUrl(configuredUrl);
			if (normalizedUrl != null) {
				System.setProperty("spring.datasource.url", normalizedUrl);
				logger.info("Datasource URL resolved from environment");
			}
			return;
		}

		String host = firstNonBlank(
				System.getenv("PGHOST"),
				System.getenv("POSTGRES_HOST"),
				System.getenv("DATABASE_HOST")
		);
		String database = firstNonBlank(
				System.getenv("PGDATABASE"),
				System.getenv("POSTGRES_DB"),
				System.getenv("DATABASE_NAME")
		);
		String portText = firstNonBlank(
				System.getenv("PGPORT"),
				System.getenv("POSTGRES_PORT"),
				System.getenv("DATABASE_PORT")
		);

		if (host == null || database == null) {
			logger.error("Datasource configuration missing. Set one of: DATABASE_URL, JDBC_DATABASE_URL, SPRING_DATASOURCE_URL, PGHOST/PGDATABASE/PGUSER/PGPASSWORD");
			return;
		}

		int port = 5432;
		if (portText != null) {
			try {
				port = Integer.parseInt(portText);
			} catch (NumberFormatException ignored) {
				port = 5432;
			}
		}

		String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
		System.setProperty("spring.datasource.url", jdbcUrl);
		logger.info("Datasource URL constructed from host/port/database environment variables");

		String username = firstNonBlank(
				System.getenv("PGUSER"),
				System.getenv("POSTGRES_USER"),
				System.getenv("DATABASE_USER"),
				System.getenv("DATABASE_USERNAME")
		);
		String password = firstNonBlank(
				System.getenv("PGPASSWORD"),
				System.getenv("POSTGRES_PASSWORD"),
				System.getenv("DATABASE_PASSWORD")
		);

		if (username != null) {
			System.setProperty("spring.datasource.username", username);
		}
		if (password != null) {
			System.setProperty("spring.datasource.password", password);
		}
	}

	private static String findDatabaseUrlFromAnyEnv() {
		for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (value == null || value.isBlank()) {
				continue;
			}

			boolean looksLikePostgresUrl = value.startsWith("jdbc:postgresql://")
					|| value.startsWith("postgres://")
					|| value.startsWith("postgresql://");

			if (!looksLikePostgresUrl) {
				continue;
			}

			String upperKey = key.toUpperCase();
			if (upperKey.contains("DATABASE") || upperKey.contains("POSTGRES") || upperKey.contains("JDBC") || upperKey.contains("DB")) {
				logger.info("Detected datasource URL in environment variable: {}", key);
				return value;
			}
		}

		return null;
	}

	private static String normalizeToJdbcUrl(String databaseUrl) {
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return null;
		}

		if (databaseUrl.startsWith("jdbc:postgresql://")) {
			return databaseUrl;
		}

		if (!(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
			return null;
		}

		URI uri = URI.create(databaseUrl);
		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String database = uri.getPath() == null ? "" : uri.getPath();

		if (host == null || database.isBlank()) {
			return null;
		}

		String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + database;
		String query = uri.getQuery();
		if (query != null && !query.isBlank()) {
			jdbcUrl = jdbcUrl + "?" + query;
		}
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

		return jdbcUrl;
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
