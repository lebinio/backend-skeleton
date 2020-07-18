package com.lebinh.skeleton.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import com.lebinh.skeleton.constant.CommonConstant;

/** Implementation of AuditorAware based on Spring Security. */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    return Optional.ofNullable(
        SecurityUtils.getCurrentUserLogin().orElse(CommonConstant.SYSTEM_ACCOUNT));
  }
}
