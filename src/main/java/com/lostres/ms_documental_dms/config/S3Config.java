package com.lostres.ms_documental_dms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.access-key:mock-access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key:mock-secret-key}")
    private String secretKey;

    @Value("${aws.s3.endpoint-url:#{null}}")
    private String endpointUrl;

    /** URL accesible desde el navegador (LocalStack en dev: http://localhost:4566) */
    @Value("${aws.s3.public-endpoint-url:#{null}}")
    private String publicEndpointUrl;

    @Value("${aws.s3.use-iam:false}")
    private boolean useIam;

    private static final S3Configuration PATH_STYLE = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(PATH_STYLE);
        applyEndpointOverride(builder, presignerEndpoint());
        return builder.build();
    }

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(PATH_STYLE);
        applyEndpointOverride(builder, endpointUrl);
        return builder.build();
    }

    private String presignerEndpoint() {
        if (publicEndpointUrl != null && !publicEndpointUrl.isEmpty()) {
            return publicEndpointUrl;
        }
        return endpointUrl;
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (useIam) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private void applyEndpointOverride(S3Presigner.Builder builder, String endpoint) {
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
    }

    private void applyEndpointOverride(S3ClientBuilder builder, String endpoint) {
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
    }
}
