package com.lostres.ms_documental_dms.config;

import com.lostres.ms_documental_dms.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3HealthIndicator implements HealthIndicator {

    private final S3Service s3Service;

    @Override
    public Health health() {
        if (s3Service.bucketExists()) {
            return Health.up().withDetail("s3", "bucket accesible").build();
        }
        return Health.down().withDetail("s3", "bucket no accesible").build();
    }
}
