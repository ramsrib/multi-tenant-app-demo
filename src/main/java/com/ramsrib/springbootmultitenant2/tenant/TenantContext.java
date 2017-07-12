package com.ramsrib.springbootmultitenant2.tenant;

public class TenantContext {

  private static ThreadLocal<String> currentTenant = new ThreadLocal<>();

  public static String getCurrentTenant() {
    return currentTenant.get();
  }

  public static void setCurrentTenant(String tenant) {
    currentTenant.set(tenant);
  }

  public static void clear() {
    currentTenant.set(null);
  }

}