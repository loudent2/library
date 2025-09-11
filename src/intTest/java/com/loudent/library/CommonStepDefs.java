package com.loudent.library;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loudent.library.util.StepHelper;
import com.loudent.library.util.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@CucumberContextConfiguration
@SpringBootTest(
    classes = LibraryApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inttest")
public class CommonStepDefs {

  private final TestContext context;
  private final ObjectMapper objectMapper;
  private final StepHelper stepHelper;
  @LocalServerPort private int port;

  /*@BeforeAll
  static void configureRestAssured(int port) {
    RestAssured.port = port;
  }*/

  @Before
  public void beforeScenario(Scenario scenario) {
    log.info("Starting scenario: {}", scenario.getName());
  }

  @After
  public void afterScenario(Scenario scenario) {
    log.info("Finished scenario: {}. Status: {}", scenario.getName(), scenario.getStatus());
  }

  public CommonStepDefs(TestContext context, ObjectMapper objectMapper, StepHelper stepHelper) {
    this.context = context;
    this.objectMapper = objectMapper;
    this.stepHelper = stepHelper;
  }

  @Given("the service is running")
  public void theServiceIsRunning() {
    RestAssured.port = port;
    Response response = given().when().get("/actuator/health").then().extract().response();
    JsonNode json = stepHelper.parseJson(response);
    assertThat(json.get("status").asText()).isEqualTo("UP");
  }

  @When("I check the health endpoint")
  public void iCheckTheHealthEndpoint() {
    Response response = given().when().get("/actuator/health").then().extract().response();
    context.setLastResponse(response);
  }

  @When("I check the info endpoint")
  public void CheckTheInfoEndpoint() {
    Response response = given().when().get("/actuator/info").then().extract().response();
    context.setLastResponse(response);
  }

  @Then("the last response status should be {int}")
  public void theLastResponseStatusShouldBe(int status) {
    context.getLastResponse().then().statusCode(status);
  }
}
