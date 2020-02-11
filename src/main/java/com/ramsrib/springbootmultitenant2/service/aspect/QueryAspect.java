package com.ramsrib.springbootmultitenant2.service.aspect;

import com.ramsrib.springbootmultitenant2.tenant.TenantContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class QueryAspect {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut("@annotation(org.springframework.data.jpa.repository.Query)")
    public void annotationPoinCut(){}


    @Before("annotationPoinCut()")
    public void  before(JoinPoint joinPoint){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Query query = method.getAnnotation(Query.class);
        //TO DO  add more common parse and enfore
        if(query.nativeQuery() && query.value().indexOf("tenant_id = ") < 0){
            log.warn("Query parameters without 'tenant_id' :" + query.value());
        }
    }
}
