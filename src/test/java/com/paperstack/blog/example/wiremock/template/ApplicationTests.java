package com.paperstack.blog.example.wiremock.template;

/*
Required only if the test configuration is being used:

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
 */

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class ApplicationTests {

	/* NOT required because we explicitly enable the response-template transformer in the test below
	@TestConfiguration
	public static class ApplicationTestConfiguration {
		@Bean
		public WireMockConfigurationCustomizer customizer() {
			return config -> config.extensions(new ResponseTemplateTransformer(true));
		}
	}
	*/

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