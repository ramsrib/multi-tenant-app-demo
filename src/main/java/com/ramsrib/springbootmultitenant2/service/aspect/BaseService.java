package com.ramsrib.springbootmultitenant2.service.aspect;

import javax.persistence.EntityManager;

public interface BaseService {
    EntityManager getEntityManager();
 }
