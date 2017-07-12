package com.ramsrib.springbootmultitenant2.model;

public interface TenantSupport {
  String getTenantId();

  void setTenantId(String tenantId);
}
