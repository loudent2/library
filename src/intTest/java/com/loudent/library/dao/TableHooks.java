package com.loudent.library.dao;

import static com.loudent.library.util.DynamoDbTestUtils.ACCOUNT_TABLE_NAME;
import static com.loudent.library.util.DynamoDbTestUtils.ACTIVITY_TABLE_NAME;
import static com.loudent.library.util.DynamoDbTestUtils.CATALOG_TABLE_NAME;

import com.loudent.library.util.DynamoDbTestUtils;
import groovy.util.logging.Slf4j;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
public class TableHooks {
  private final DynamoDbTestUtils ddbUtils;

  @Before("@catalog")
  public void beforeCatalog() {
    ddbUtils.createCatalogTable();
    ddbUtils.createActivityTable();
  }

  @After("@catalog")
  public void afterCatalog() {
    ddbUtils.deleteTable(CATALOG_TABLE_NAME);
    ddbUtils.deleteTable(ACTIVITY_TABLE_NAME);
  }

  @Before("@account")
  public void beforeAccount() {
    ddbUtils.createAccountTable();
    ddbUtils.createActivityTable();
  }

  @After("@account")
  public void afterAccount() {
    ddbUtils.deleteTable(ACCOUNT_TABLE_NAME);
    ddbUtils.deleteTable(ACTIVITY_TABLE_NAME);
  }

  @Before("@flow")
  public void beforeFlow() {
    ddbUtils.createAllTables();
  }

  @After("@flow")
  public void afterFlow() {
    ddbUtils.deleteAllTables();
  }
}
