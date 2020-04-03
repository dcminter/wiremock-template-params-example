# Wiremock Template Parameters (and Spring Boot testing)

Explaining and demonstrating the use of template parameters in wiremocked responses within
a Spring Boot based integration test.

*This article is available on both my 
blog [Paperstack](https://paperstack.com/post/2020/04/03/wiremock-template-parameters/) and 
[on my GitHub account](https://github.com/dcminter/wiremock-template-params-example/blob/master/README.md) (which also contains all 
of the source code).*

## Contents

* [What is Wiremock?](#what-is-wiremock)
* [What is RestAssured?](#what-is-restassured)
* [The application under test](#the-application-under-test)
* [Port management](#port-management)
* [Using templates](#using-templates)
* [Afterthoughts](#afterthoughts)
* [Source documentation](#source-documentation)
* [Annoyances and gotchas](#annoyances-and-gotchas)
* [About the author](#about-the-author)


## What is Wiremock?

For the purposes of this article, [Wiremock](http://wiremock.org/) is a set of libraries to allow you to mock up web 
APIs for testing. There's more to it than that, but this is an area in which it shines. 

The typical scenario - and what I'll be demonstrating here - is where you are creating a service that has additional 
API dependencies. A good integration test needs those services to be completely under the control of your tests so
that you can simulate responses, verify the expected requests are made, and simulate error situations.

The example in the test case here:
```java
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
```
I'll break this down in a moment, but in brief this sets up an HTTP server and waits for `GET` requests to the `/ping`
endpoint - at which point it returns the response created from a file `pong.json` supplemented by a `my-value` parameter
set to `pong`. Details follow a little further on. 

## What is RestAssured?

For the other side of the coin you want to be able to make requests to your own services and verify that they return
the expected responses. [RestAssured](http://rest-assured.io/) is a library that gives you a Domain Specific 
Language (DSL) to make the requests and validate the responses.

The example in the test case here:
```java
when().
    get(uri).
then().
    statusCode(200).body(is("Pingy!"));
```
This sends a `GET` request to an endpoint (defined by the `uri` string supplied) and expects a response with
status code `200` (success with content) and the body contains just the string `Pingy!`. If any of these conditions 
fail to hold then an `AssertionError` will be thrown describing the discrepancy and the test will fail.

## The application under test

In the scenario I'm considering I have a piece of middleware that has transitive dependencies and I want to test its 
interactions with those dependencies. The relationship will be: rolls up sleeves and creates ascii art...

```text
+-----------+      +--------------------------+      +------------------+
|           |      |                          |      |                  |
| Test Case +----->| Application (Middleware) +----->| API (dependency) |
|           |      |                          |      |                  |
+-----------+      +--------------------------+      +------------------+
```

My demonstration application will do the following:

 * Accept `GET` requests on path `/pingalyse`
 * Make a `GET` request to the configured API endpoint
 * Expect the response from the API endpoint to be a JSON object of the form `{ "response" : "pong" }`
 * If the response from the API endpoint is as expected
   * It will reply to the original request with "Pingy!"
   
The significant part of the controller of the application is therefore as follows:
```java
@GetMapping("/pingalyse")
public String pingalyse() {
    final Optional<Ping> ping = Optional.of(restTemplate.getForObject(properties.getTargetUri(), Ping.class));
    return ping.map(p -> respond(p.getResponse())).orElse("No ping payload :'(");
}

private String respond(final String responseText) {
    return "pong".equals(responseText) ? "Pingy!" : "Not so pingy :'(";
}
```
As you an see, if this gets the expected payload it returns the text `Pingy!`. Other than a minor effort to deal with null values
there is no real error checking here. For example, if the API dependency is not running (whether real or mocked) then 
the controller will fail, an exception will be thrown, and my service will return a `500` status response.
  
## Port management

One way to configure this test would be to hard code a port for my application - say the default Spring Boot port 
of 8080 - and hardcode another port for the Wiremocked API - say port 8082. This would work fine, but would
make it impossible to run the test if some other application had already reserved those ports.

A better strategy is to instruct both the application and the wiremocked API to choose unused ephemeral ports. Spring Boot
supports this approach nicely. Firstly I can configure the `SpringBootTest` class to choose a suitable unused 'random' port:
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ApplicationTests {
...
}
```
I can also tell the SpringBoot test that I intend to use Wiremock (saving some of the configuration work) and that
I want it to choose a suitable port as well by specifying the port as zero (zero is not a real port so this is unambiguous):
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class ApplicationTests {
...    
}
``` 
Spring Boot Test makes the special property `wiremock.server.port` available at configuration time to the Spring Boot
application. I can therefore specify my test uri property like this:
```
pingalyser.targetUri=http://localhost:${wiremock.server.port}/ping
```
Finally I need to know what port the application under test will be listening on - and Spring Boot Test provides 
the `@LocalServerPort` annotation to autowire this into the test:
```java
@LocalServerPort
private int serverPort;
```
## Using templates

Templates are not enabled by default in Wiremock so you need to turn them on globally (for all wiremock stubs you 
create) or for an individual case.

To turn them on globally you would have to override the default Spring Boot Test configuration of Wiremock by supplying a 
configuration bean as part of the test suite:
```java
@TestConfiguration
public static class ApplicationTestConfiguration {
    @Bean
    public WireMockConfigurationCustomizer customizer() {
        return config -> config.extensions(new ResponseTemplateTransformer(true));
    }
}
```

In my example I turn the transformer on explicitly for the test case with the Wiremock DSL 
invocation of `.withTransformers("response-template")`.

I specify the template file to use by invoking `.withBodyFile("pong.json")` and by default wiremock will search the
classpath for this file under a `__files` path. The project path to this file is therefore `src/test/resources/__file/pong.json` 
and it contains the following:
```json
{
  "response" : "{{parameters.my-value}}"
}
```
The Wiremock response template transformer uses the [Mustache](https://mustache.github.io/) templating format and so I
 need to supply a model with `parameters.my-value` populated.

**Note** that in my call to populate this I omit the `parameters` prefix I **omit the parameters prefix** but this
**must** be present in the template file itself. This isn't super clear on the Wiremock documentation!

Another set of model keys is available and automatically created from the request under the `request` prefix, which is 
why a prefix is needed to differentiate the two sets.

In the light of this, the invocation of `.withTransformerParameter("my-value", "pong")` obviously sets the key in the
model to `pong` and the payload from wiremock will be:
```java
{
  "response" : "pong"
}
``` 
This is exactly what the `pingRespondsOk` test is looking for, so the test will pass.

## Afterthoughts

This is of course a super simple demonstration of just one part of the capabilities of Wiremock in a particular 
context, but it has a lot of features that I like a lot. Amongst other things you can run it stand-alone as its
own process and controll it remotely via an admin API.

## Source documentation

 * [Wiremock Docs](http://wiremock.org/docs/response-templating/): http://wiremock.org/docs/response-templating/
 * [Spring Docs](https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#features-wiremock): https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#features-wiremock
 * [Rest Assured Docs](http://rest-assured.io/): http://rest-assured.io/

## Annoyances and gotchas

Since this build uses JDK11 (the current long-term-support version) we're in the post-Java9 "Jigsaw" era of modules and 
a lot of libraries still have problems with that. At test start-up I see the following warnings:
```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.codehaus.groovy.reflection.CachedClass (file:/home/dcminter/.m2/repository/org/codehaus/groovy/groovy/2.5.10/groovy-2.5.10.jar) to method java.lang.Object.finalize()
WARNING: Please consider reporting this to the maintainers of org.codehaus.groovy.reflection.CachedClass
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```
This is coming from a dependency of the RestAssured library. I turn I'm using an older version of RestAssured itself 
because of some other issues that seem to be related to modules:
 
 * https://github.com/rest-assured/rest-assured/issues/1175
 * https://github.com/rest-assured/rest-assured/issues/1220

The suggested work-arounds aren't fixing things for me, so I've downgraded to an older version that works (but 
with the above warnings). At the time of writing the latest version of RestAssured is 4.3.0 so if a more recent
release is available you should give that a whirl (and I intend to update this codebase if I see that get 
fixed). You may need to use the `rest-assured-all` artifact rather than `rest-assured` in that case.

## About the author

Dave Minter is a consultant with [Diabol AB](http://diabol.se/) based in Stockholm, Sweden.