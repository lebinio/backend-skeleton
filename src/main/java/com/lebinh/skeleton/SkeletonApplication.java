package com.lebinh.skeleton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import com.lebinh.skeleton.config.ApplicationProperties;

@ComponentScan
@EnableAutoConfiguration
@EnableConfigurationProperties({ApplicationProperties.class})
public class SkeletonApplication {
  public static void main(String[] args) {
    SpringApplication.run(SkeletonApplication.class, args);
  }
}
