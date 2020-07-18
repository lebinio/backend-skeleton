package com.lebinh.skeleton.config;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;
import com.lebinh.skeleton.security.AuthoritiesConstants;
import com.lebinh.skeleton.security.jwt.JWTConfigurer;
import com.lebinh.skeleton.security.jwt.JwtAccessDeniedHandler;
import com.lebinh.skeleton.security.jwt.JwtAuthenticationEntryPoint;
import com.lebinh.skeleton.security.jwt.TokenProvider;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  private final AuthenticationManagerBuilder authenticationManagerBuilder;

  private final UserDetailsService userDetailsService;

  private final TokenProvider tokenProvider;

  private final CorsFilter corsFilter;

  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
  
  public SecurityConfiguration(
      AuthenticationManagerBuilder authenticationManagerBuilder,
      UserDetailsService userDetailsService,
      TokenProvider tokenProvider,
      CorsFilter corsFilter,
      JwtAuthenticationEntryPoint authenticationEntryPoint,
      JwtAccessDeniedHandler jwtAccessDeniedHandler) {
    this.authenticationManagerBuilder = authenticationManagerBuilder;
    this.userDetailsService = userDetailsService;
    this.tokenProvider = tokenProvider;
    this.corsFilter = corsFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
  }

  @PostConstruct
  public void init() {
    try {
      authenticationManagerBuilder
          .userDetailsService(userDetailsService)
          .passwordEncoder(passwordEncoder());
    } catch (Exception e) {
      throw new BeanInitializationException("Security configuration failed", e);
    }
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring()
        .antMatchers(HttpMethod.OPTIONS, "/**")
        .antMatchers("/app/**/*.{js,html}")
        .antMatchers("/i18n/**")
        .antMatchers("/content/**")
        .antMatchers("/swagger-ui/index.html")
        .antMatchers("/test/**")
        .antMatchers("/h2-console/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling()
        .authenticationEntryPoint(authenticationEntryPoint)
        .accessDeniedHandler(jwtAccessDeniedHandler)
        .and()
        .csrf()
        .disable()
        .headers()
        .frameOptions()
        .disable()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/api/register")
        .permitAll()
        .antMatchers("/api/activate")
        .permitAll()
        .antMatchers("/api/authenticate")
        .permitAll()
        .antMatchers("/api/account/reset-password/init")
        .permitAll()
        .antMatchers("/api/account/reset-password/finish")
        .permitAll()
        .antMatchers("/api/profile-info")
        .permitAll()
        .antMatchers("/api/**")
        .authenticated()
        .antMatchers("/management/health")
        .permitAll()
        .antMatchers("/management/**")
        .hasAuthority(AuthoritiesConstants.ADMIN)
        .antMatchers("/v2/api-docs/**")
        .permitAll()
        .antMatchers("/swagger-resources/configuration/ui")
        .permitAll()
        .antMatchers("/swagger-ui/index.html")
        .hasAuthority(AuthoritiesConstants.ADMIN)
        .and()
        .apply(securityConfigurerAdapter());
  }

  private JWTConfigurer securityConfigurerAdapter() {
    return new JWTConfigurer(tokenProvider);
  }
}