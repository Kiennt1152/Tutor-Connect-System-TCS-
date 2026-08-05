package com.tcs.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.validate-on-migrate:true}") boolean validateOnMigrate) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .outOfOrder(true)
                .validateOnMigrate(validateOnMigrate)
                .load();
    }

    @Configuration(proxyBeanMethods = false)
    static class JpaDependsOnFlyway extends EntityManagerFactoryDependsOnPostProcessor {

        JpaDependsOnFlyway() {
            super("flyway");
        }
    }
}
