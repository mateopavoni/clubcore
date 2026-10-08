package com.clubcore.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los datos demo (db/seed: admin con contraseña pública, planes y clientes de ejemplo) solo
 * pueden cargarse en el perfil dev. Si prod los incluyera, cualquier base nueva en producción
 * nacería con un ADMIN de contraseña conocida.
 */
class SeedScopeTest {

    private static String flywayLocations(String profileFile) {
        return prop(profileFile, "spring.flyway.locations");
    }

    private static String prop(String profileFile, String key) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(profileFile));
        Properties props = yaml.getObject();
        assertThat(props).as("no se pudo leer %s", profileFile).isNotNull();
        return props.getProperty(key);
    }

    @Test
    void prodNoCargaElSeedDemo() {
        assertThat(flywayLocations("application-prod.yml"))
                .doesNotContain("seed");
    }

    @Test
    void devSiCargaElSeedDemo() {
        assertThat(flywayLocations("application-dev.yml")).contains("db/seed");
    }

    @Test
    void prodApagaSwaggerPorDefecto() {
        assertThat(prop("application-prod.yml", "springdoc.swagger-ui.enabled")).contains(":false");
        assertThat(prop("application-prod.yml", "springdoc.api-docs.enabled")).contains(":false");
    }

    @Test
    void prodNoExponeActuatorInfo() {
        assertThat(prop("application-prod.yml", "management.endpoints.web.exposure.include"))
                .contains("health")
                .doesNotContain("info");
    }
}
