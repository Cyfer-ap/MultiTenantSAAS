package com.chacha.multitenantsaas.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chacha.multitenantsaas.config.R2StorageConfiguration;
import com.chacha.multitenantsaas.config.StorageProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class R2ObjectStorageServiceTest {

    @Test
    void generatesPathStylePresignedUrlsWithoutCallingR2() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("r2");
        properties.getR2().setEndpoint("https://example.r2.cloudflarestorage.com");
        properties.getR2().setBucket("multitenantsaas-attachments");
        properties.getR2().setRegion("auto");
        properties.getR2().setAccessKeyId("test-access-key");
        properties.getR2().setSecretAccessKey("test-secret-key");

        R2StorageConfiguration configuration = new R2StorageConfiguration(properties);

        try (S3Client s3Client = configuration.r2S3Client();
                S3Presigner presigner = configuration.r2S3Presigner()) {
            R2ObjectStorageService service =
                    new R2ObjectStorageService(properties, s3Client, presigner);

            ObjectStorageService.PresignedUrl upload =
                    service.presignUpload("tenants/t-1/tasks/task-1/file.txt", "text/plain");
            ObjectStorageService.PresignedUrl download =
                    service.presignDownload("tenants/t-1/tasks/task-1/file.txt");

            assertTrue(upload.url().startsWith("https://example.r2.cloudflarestorage.com/"));
            assertTrue(upload.url().contains("multitenantsaas-attachments"));
            assertTrue(upload.url().contains("file.txt"));
            assertTrue(upload.url().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
            assertEquals("text/plain", upload.requiredHeaders().get("Content-Type"));

            assertTrue(download.url().startsWith("https://example.r2.cloudflarestorage.com/"));
            assertTrue(download.url().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
            assertTrue(download.requiredHeaders().isEmpty());
        }
    }
}
