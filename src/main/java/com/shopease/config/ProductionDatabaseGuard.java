package com.shopease.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Runs before the database connection is opened. In production, a database address that still points at
 * "localhost" means the DB_* settings were not provided, so stop with a clear message instead of a long stack trace.
 */
@Component
public class ProductionDatabaseGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (url.contains("localhost") || url.contains("127.0.0.1")) {
            throw new IllegalStateException(
                    "Database is not configured for production (the address is still " + url + "). "
                            + "Set DB_HOST, DB_PORT, DB_NAME, DB_USERNAME and DB_PASSWORD (or one DB_URL like "
                            + "jdbc:postgresql://HOST:5432/DATABASE) on your hosting service.");
        }
    }
}
