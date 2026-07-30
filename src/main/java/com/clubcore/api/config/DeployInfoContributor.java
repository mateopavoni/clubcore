package com.clubcore.api.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dokku construye la imagen sin el directorio .git (git-properties no tiene de donde leer
 * ahi), asi que /actuator/info queda vacio en ese entorno. Este contributor cubre ese caso
 * leyendo GIT_REV, que el pipeline de CI setea como config var antes de cada deploy.
 */
@Component
public class DeployInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        String commit = System.getenv("GIT_REV");
        if (commit == null || commit.isBlank()) {
            return;
        }
        Map<String, String> deploy = new LinkedHashMap<>();
        deploy.put("commit", commit.length() > 7 ? commit.substring(0, 7) : commit);
        deploy.put("full", commit);
        builder.withDetail("deploy", deploy);
    }
}
