package com.loudent.library.util;

import com.loudent.library.dao.account.Account;
import com.loudent.library.dao.activity.Activity;
import com.loudent.library.dao.catalog.Catalog;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Slf4j
@Data
public class TestContext {
  @Autowired private DynamoDbEnhancedClient enhancedClient;

  private Response lastResponse;
  private final Map<String, Object> state = new HashMap<>();
  private List<Catalog> expectedBooks = new ArrayList<>();
  private List<Account> expectedAccounts = new ArrayList<>();
  private List<Activity> expectedActivities = new ArrayList<>();

  public void put(String key, Object value) {
    state.put(key, value);
  }

  public <T> T get(String key, Class<T> type) {
    return type.cast(state.get(key));
  }

  public boolean has(String key) {
    return state.containsKey(key);
  }
}
