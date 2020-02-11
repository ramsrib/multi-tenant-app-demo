package com.ramsrib.springbootmultitenant2.tenant;

import com.ramsrib.springbootmultitenant2.model.TenantSupport;
import org.hibernate.EmptyInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.hibernate.type.Type;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MultiTenantInterceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Bean
    public EmptyInterceptor hibernateInterceptor() {
        return new EmptyInterceptor() {

            @Override
            public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
                if (entity instanceof TenantSupport) {
                    ((TenantSupport) entity).setTenantId(TenantContext.getCurrentTenant());
                    log.debug("[delete] Updating the entity " + id + " with tenant information: " + TenantContext.getCurrentTenant());
                }
            }


            @Override
            public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
                if (entity instanceof TenantSupport) {
                    ((TenantSupport) entity).setTenantId(TenantContext.getCurrentTenant());
                    log.debug("[flush-dirty] Updating the entity " + id + " with tenant information: " + TenantContext.getCurrentTenant());
                }
                return false;
            }

            @Override
            public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
                if (entity instanceof TenantSupport) {
                    ((TenantSupport) entity).setTenantId(TenantContext.getCurrentTenant());
                    log.debug("[save] Updating the entity " + id + " with tenant information: " + TenantContext.getCurrentTenant());
                }
                return false;
            }

            @Override
            public String onPrepareStatement(String sql) {
                return sql;
            }
        };
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factory, DataSource dataSource, JpaProperties properties) {
        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.putAll(properties.getHibernateProperties(new HibernateSettings()));
        jpaProperties.put("hibernate.ejb.interceptor", hibernateInterceptor());
        return factory.dataSource(dataSource).packages("com.ramsrib").properties(jpaProperties).build();
    }
}
