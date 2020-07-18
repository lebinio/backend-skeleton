package com.lebinh.skeleton.security.jwt;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final HandlerExceptionResolver resolver;

  @Autowired
  public JwtAccessDeniedHandler(
      @Qualifier("handlerExceptionResolver") final HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void handle(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AccessDeniedException exception)
      throws IOException, ServletException {
    resolver.resolveException(request, response, null, exception);
  }
}
