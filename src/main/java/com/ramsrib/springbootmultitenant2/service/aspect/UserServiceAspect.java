package com.ramsrib.springbootmultitenant2.service.aspect;

import com.ramsrib.springbootmultitenant2.service.UserService;
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
public class UserServiceAspect {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  // only applicable to user service
  @Before("execution(* com.ramsrib.springbootmultitenant2.service.UserService.*(..)) && !execution(* com.ramsrib.springbootmultitenant2.service.UserService.run(..)) && target(userService)")
  public void aroundExecution(JoinPoint pjp, UserService userService) throws Throwable {
    org.hibernate.Filter filter = userService.entityManager.unwrap(Session.class).enableFilter("tenantFilter");
    filter.setParameter("tenantId", TenantContext.getCurrentTenant());
    filter.validate();
  }
}
