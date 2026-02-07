package com.courseplatform.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

	@Value("${spring.elasticsearch.uris}")
	private String uri;

	@Value("${spring.elasticsearch.username:}")
	private String username;

	@Value("${spring.elasticsearch.password:}")
	private String password;

	@Override
	@NonNull
	public ClientConfiguration clientConfiguration() {
		// 1. SMART PARSING: Handle both "http://host:port" and "host:port"
		String hostAndPort = uri.replace("https://", "").replace("http://", "");

		// Remove trailing slash if present (Common error source)
		if (hostAndPort.endsWith("/")) {
			hostAndPort = hostAndPort.substring(0, hostAndPort.length() - 1);
		}

		log.info("----------------------------------------------------------------");
		log.info("ELASTICSEARCH CONNECTION DEBUG:");
		log.info("Original URI: {}", uri);
		log.info("Parsed Host:  {}", hostAndPort);
		log.info("Username:     {}", (username == null || username.isBlank()) ? "NONE" : username);
		log.info("Password:     {}", (password == null || password.isBlank()) ? "NONE" : password);
		log.info("Environment:  {}", hostAndPort.contains("localhost") ? "LOCAL" : "DOCKER/REMOTE");
		log.info("----------------------------------------------------------------");

		// 3. BUILDER CHAINING
		var connectionBuilder = ClientConfiguration.builder()
			.connectedTo(hostAndPort);

		// 4. SSL LOGIC (Only enable if URI explicitly says HTTPS)
		if (uri.toLowerCase().startsWith("https")) {
			// ---- HTTPS ENABLED ----
			log.info("Enabling SSL/TLS for Elasticsearch connection.");
			var sslBuilder = connectionBuilder.usingSsl(buildUnsafeSslContext());

			if ( hasAuth() ) {
				log.info("Using Basic Authentication with SSL.");
				return sslBuilder.withBasicAuth(username, password)
					.withConnectTimeout(java.time.Duration.ofSeconds(10))
					.withSocketTimeout(java.time.Duration.ofSeconds(30))
					.build();
			} else {
				log.info("No authentication provided, but SSL is enabled. Proceeding without auth.");
				return sslBuilder
					.withConnectTimeout(java.time.Duration.ofSeconds(10))
					.withSocketTimeout(java.time.Duration.ofSeconds(30))
					.build();
			}
		} else {
			// ---- HTTP ENABLED ----
			log.info("Using Plaintext (HTTP) for Elasticsearch connection.");
			if ( hasAuth() ) {
				log.info("Using Basic Authentication without SSL.");
				return connectionBuilder.withBasicAuth(username, password)
					.withConnectTimeout(java.time.Duration.ofSeconds(10))
					.withSocketTimeout(java.time.Duration.ofSeconds(30))
					.build();
			} else {
				log.info("No authentication provided, using plaintext HTTP.");
				return connectionBuilder
					.withConnectTimeout(java.time.Duration.ofSeconds(10))
					.withSocketTimeout(java.time.Duration.ofSeconds(30))
					.build();
			}
		}
	}

	private boolean hasAuth() {
		return username != null && !username.isBlank() && password != null && !password.isBlank();
	}

	// Helper for self-signed certs (Docker dev environments)
	private SSLContext buildUnsafeSslContext() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public void checkClientTrusted(X509Certificate[] chain, String authType) {}
					public void checkServerTrusted(X509Certificate[] chain, String authType) {}
					public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0];}
				}
			};
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			return sc;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create unsafe SSL context", e);
		}
	}
}