package com.ramsrib.springbootmultitenant2.filter;

import com.ramsrib.springbootmultitenant2.tenant.TenantContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TenantFilter implements Filter {

  private static final String TENANT_HEADER = "X-TenantID";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    HttpServletRequest request = (HttpServletRequest) servletRequest;

    if(request.getRequestURI().indexOf("/actuator/") >= 0) {
      TenantContext.setCurrentTenant("actuator");
    }else {
      String tenantHeader = request.getHeader(TENANT_HEADER);
      if (tenantHeader != null && !tenantHeader.isEmpty()) {
        TenantContext.setCurrentTenant(tenantHeader);
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\": \"No tenant supplied\"}");
        response.getWriter().flush();
        return;
      }
    }


    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
  }
}
