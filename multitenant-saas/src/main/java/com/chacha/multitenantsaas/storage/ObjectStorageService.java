package com.chacha.multitenantsaas.storage;

import java.time.Instant;
import java.util.Map;

public interface ObjectStorageService {

    PresignedUrl presignUpload(String objectKey, String contentType);

    PresignedUrl presignDownload(String objectKey);

    ObjectMetadata getObjectMetadata(String objectKey);

    void deleteObject(String objectKey);

    record PresignedUrl(String url, Instant expiresAt, Map<String, String> requiredHeaders) {

        public PresignedUrl {
            requiredHeaders = Map.copyOf(requiredHeaders);
        }
    }

    record ObjectMetadata(long sizeBytes, String contentType, String eTag, Instant lastModified) {}
}
