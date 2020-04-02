package com.paperstack.blog.example.wiremock.template;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class ApplicationTests {

	private static class ApplicationTestConfiguration {
		@Bean
		public WireMockConfigurationCustomizer customizer() {
			// Because we're setting the "global" flag to false in the ResponseTemplateTransformer's constructor
			// our stubs will need to explicitly enable the "response-template" transformers (see below)
			return config -> config.extensions(new ResponseTemplateTransformer(false));
		}
	}

	@LocalServerPort
	private int serverPort;

	@Test
	void pingRespondsOk() {
		stubFor(
			get(
				urlPathEqualTo("/ping")
			)
			.willReturn(
				aResponse()
					.withBodyFile("pong.json")
					.withHeader("Content-Type", "application/json")
					.withTransformers("response-template") // Explicitly enable the "response-template" transformer
					.withTransformerParameter("my-value", "pong")
					.withStatus(200)
			)
		);

		final String uri = String.format("http://localhost:%d/pingalyse", serverPort);

		when().
			get(uri).
		then().
			statusCode(200).body(is("Pingy!"));
	}
}