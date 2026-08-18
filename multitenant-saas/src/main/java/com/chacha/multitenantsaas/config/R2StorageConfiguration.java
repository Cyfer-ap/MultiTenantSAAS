package com.chacha.multitenantsaas.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class R2StorageConfiguration {

    private final StorageProperties storageProperties;

    public R2StorageConfiguration(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean
    public S3Client r2S3Client() {
        StorageProperties.R2 r2 = validatedR2Properties();

        return S3Client.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .credentialsProvider(credentialsProvider(r2))
                .region(Region.of(r2.getRegion()))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build())
                .build();
    }

    @Bean
    public S3Presigner r2S3Presigner() {
        StorageProperties.R2 r2 = validatedR2Properties();

        return S3Presigner.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .credentialsProvider(credentialsProvider(r2))
                .region(Region.of(r2.getRegion()))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private StorageProperties.R2 validatedR2Properties() {
        StorageProperties.R2 r2 = storageProperties.getR2();

        requireConfigured("R2_ENDPOINT", r2.getEndpoint());
        requireConfigured("R2_BUCKET", r2.getBucket());
        requireConfigured("R2_REGION", r2.getRegion());
        requireConfigured("R2_ACCESS_KEY_ID", r2.getAccessKeyId());
        requireConfigured("R2_SECRET_ACCESS_KEY", r2.getSecretAccessKey());

        if (r2.getUploadUrlExpirationMinutes() <= 0) {
            throw new IllegalStateException(
                    "R2_UPLOAD_URL_EXPIRATION_MINUTES must be greater than zero");
        }
        if (r2.getDownloadUrlExpirationMinutes() <= 0) {
            throw new IllegalStateException(
                    "R2_DOWNLOAD_URL_EXPIRATION_MINUTES must be greater than zero");
        }

        return r2;
    }

    private StaticCredentialsProvider credentialsProvider(StorageProperties.R2 r2) {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey());
        return StaticCredentialsProvider.create(credentials);
    }

    private void requireConfigured(String environmentVariable, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    environmentVariable + " must be configured when STORAGE_PROVIDER=r2");
        }
    }
}
