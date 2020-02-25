README
======

*Sample Application to demonstrate Multi-tenancy based on discriminator field using Spring Boot & Hibernate.*

Since hibernate (5.x) has some issues (refer the link below) in supporting the 'Discriminator' based multi-tenancy strategy. This following workaround will help to achieve the same.

Approach:

* Uses hibernate filter to limit the query results based on tenant.
* Uses hibernate interceptors to enforce tenant details during creating/updating entities.
* Uses Spring AOP (AspectJ) to set the filter parameters.


Explanation: 

* Each request goes thru a custom servlet filter which checks for `X-TenantID` http header and set's it in the ThreadLocal variable using `TenantContext` class. If http header is not present in request, it'll be rejected.
* Controller routes the request to Service class and the Spring AOP (`UserServiceAspect` class) intercepts the service call and set's the hibernate tenant filter.
* All the service method has to be annotated with `@Transactional` for `UserServiceAspect` to work.
* Above method works only for read queries, for write queries, we have to use hibernate interceptors.
* Custom Entity interceptor (using `EmptyInterceptor`) class which sets the tenantId value during the save/delete/flush-dirty entity events.
* Entity class should implement `TenantSupport` interface for the Entity interceptor to work. 

Usage：
* curl -X POST   http://localhost:8080/ -H 'Content-Type: application/json' -H 'X-TenantID: test2' -d '{"firstName":"king","username":"wangsoft","lastName":"soft"}'
* curl -X GET   http://localhost:8080/ -H 'Content-Type: application/json' -H 'X-TenantID: test2'
* curl -X POST   http://localhost:8080/ -H 'Content-Type: application/json' -H 'X-TenantID: test3' -d '{"firstName":"working","username":"wangsoft","lastName":"now"}'
* curl -X GET   http://localhost:8080/ -H 'Content-Type: application/json' -H 'X-TenantID: test3'


Refer:
* https://hibernate.atlassian.net/browse/HHH-6054
* https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#multitenacy
* https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#mapping-column-filter
* https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#events
