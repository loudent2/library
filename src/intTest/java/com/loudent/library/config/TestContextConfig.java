package com.loudent.library.config;

import com.loudent.library.util.TestContext;
import io.cucumber.spring.ScenarioScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestContextConfig {

  @Bean
  @ScenarioScope
  public TestContext testContext() {
    return new TestContext();
  }
}
