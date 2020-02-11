package com.ramsrib.springbootmultitenant2.service.aspect;

import com.ramsrib.springbootmultitenant2.tenant.TenantContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceAspect {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Before("execution(* com.ramsrib.springbootmultitenant2.service.*Service.*(..)) && !execution(* com.ramsrib.springbootmultitenant2.service.*Service.run(..)) && target(baseService)")
  public void aroundExecution(JoinPoint pjp, BaseService baseService) throws Throwable {
    org.hibernate.Filter filter = baseService.getEntityManager().unwrap(Session.class).enableFilter("tenantFilter");
    filter.setParameter("tenantId", TenantContext.getCurrentTenant());
//    log.info("ServiceAspect run");
    filter.validate();
  }
}
