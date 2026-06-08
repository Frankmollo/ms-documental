package com.lostres.ms_documental_dms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "dms")
@Getter
@Setter
public class DmsProperties {

    private Security security = new Security();
    private Upload upload = new Upload();
    private Cleanup cleanup = new Cleanup();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Security {
        private boolean enabled = false;
        private boolean enforceApiKey = false;
        private String apiKey = "";
        private boolean requireUserId = true;
    }

    @Getter
    @Setter
    public static class Upload {
        private long maxSizeBytes = 52_428_800L;
        private boolean requireMeterId = false;
        private String allowedContentTypes = "application/pdf,image/jpeg,image/png,image/tiff,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword,application/acad,image/vnd.dwg,application/dwg";
    }

    @Getter
    @Setter
    public static class Cleanup {
        private boolean enabled = true;
        private int pendingExpirationHours = 24;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerWindow = 60;
        private int windowSeconds = 60;
    }

    public List<String> getAllowedContentTypesList() {
        return Arrays.stream(upload.getAllowedContentTypes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
