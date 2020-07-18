package com.lebinh.skeleton.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to skeleton.
 * <p>
 * Properties are configured in the application.yml file.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

}
