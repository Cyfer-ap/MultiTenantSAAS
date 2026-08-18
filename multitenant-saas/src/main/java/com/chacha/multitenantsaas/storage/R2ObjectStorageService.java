package com.chacha.multitenantsaas.storage;

import com.chacha.multitenantsaas.config.StorageProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class R2ObjectStorageService implements ObjectStorageService {

    private final StorageProperties.R2 properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public R2ObjectStorageService(
            StorageProperties storageProperties, S3Client s3Client, S3Presigner presigner) {
        this.properties = storageProperties.getR2();
        this.s3Client = s3Client;
        this.presigner = presigner;
    }

    @Override
    public PresignedUrl presignUpload(String objectKey, String contentType) {
        validateObjectKey(objectKey);
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("contentType must not be blank");
        }

        Duration duration = Duration.ofMinutes(properties.getUploadUrlExpirationMinutes());
        PutObjectRequest objectRequest =
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .putObjectRequest(objectRequest)
                        .build();

        String url = presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUrl(
                url, Instant.now().plus(duration), Map.of("Content-Type", contentType));
    }

    @Override
    public PresignedUrl presignDownload(String objectKey) {
        validateObjectKey(objectKey);

        Duration duration = Duration.ofMinutes(properties.getDownloadUrlExpirationMinutes());
        GetObjectRequest objectRequest =
                GetObjectRequest.builder().bucket(properties.getBucket()).key(objectKey).build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(objectRequest)
                        .build();

        String url = presigner.presignGetObject(presignRequest).url().toString();
        return new PresignedUrl(url, Instant.now().plus(duration), Map.of());
    }

    @Override
    public ObjectMetadata getObjectMetadata(String objectKey) {
        validateObjectKey(objectKey);

        HeadObjectResponse response;
        try {
            response =
                    s3Client.headObject(
                            HeadObjectRequest.builder()
                                    .bucket(properties.getBucket())
                                    .key(objectKey)
                                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new IllegalArgumentException("Uploaded object was not found in storage");
            }
            throw exception;
        }

        return new ObjectMetadata(
                response.contentLength(),
                response.contentType(),
                response.eTag(),
                response.lastModified());
    }

    @Override
    public void deleteObject(String objectKey) {
        validateObjectKey(objectKey);

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .build());
    }

    private void validateObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }
        if (objectKey.startsWith("/") || objectKey.contains("..")) {
            throw new IllegalArgumentException("objectKey contains an unsafe path segment");
        }
    }
}
