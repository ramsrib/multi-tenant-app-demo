package com.ramsrib.springbootmultitenant2;

import com.ramsrib.springbootmultitenant2.model.TenantSupport;
import com.ramsrib.springbootmultitenant2.tenant.TenantContext;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factory, DataSource dataSource, JpaProperties properties) {
    Map<String, Object> jpaProperties = new HashMap<>();
    jpaProperties.putAll(properties.getHibernateProperties(new HibernateSettings()));
    jpaProperties.put("hibernate.ejb.interceptor", hibernateInterceptor());
    return factory.dataSource(dataSource).packages("com.ramsrib").properties(jpaProperties).build();
  }

  @Bean
  public EmptyInterceptor hibernateInterceptor() {
    return new EmptyInterceptor() {

      @Override
      public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
          log.debug("[save] Updating the entity " + id + " with tenant information: " + TenantContext.getCurrentTenant());
          ((TenantSupport) entity).setTenantId(TenantContext.getCurrentTenant());
        }
        return false;
      }

      @Override
      public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
          log.debug("[delete] Updating the entity " + id + " with tenant information: " + TenantContext.getCurrentTenant());
          ((TenantSupport) entity).setTenantId(TenantContext.getCurrentTenant());
        }
      }

      @Override
      public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
          log.debug("[flush-dirty] Updating the entity " + id + " with tenant information: " + TenantContext.getCurrentTenant());
          ((TenantSupport) entity).setTenantId(TenantContext.getCurrentTenant());
        }
        return false;
      }

    };
  }

}
