package com.lostres.ms_documental_dms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityStartupValidator {

    private final DmsProperties dmsProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityPolicy() {
        if (dmsProperties.getSecurity().isEnforceApiKey()
                && (dmsProperties.getSecurity().getApiKey() == null
                || dmsProperties.getSecurity().getApiKey().isBlank())) {
            throw new IllegalStateException(
                    "ENFORCE_API_KEY está activo pero SERVICE_API_KEY no está configurada."
            );
        }
        log.info("Política de seguridad DMS: enabled={}, enforceApiKey={}, requireUserId={}, requireMeterId={}",
                dmsProperties.getSecurity().isEnabled(),
                dmsProperties.getSecurity().isEnforceApiKey(),
                dmsProperties.getSecurity().isRequireUserId(),
                dmsProperties.getUpload().isRequireMeterId());
    }
}
