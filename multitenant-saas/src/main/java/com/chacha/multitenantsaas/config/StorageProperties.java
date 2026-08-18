package com.chacha.multitenantsaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "disabled";
    private R2 r2 = new R2();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public R2 getR2() {
        return r2;
    }

    public void setR2(R2 r2) {
        this.r2 = r2;
    }

    public static class R2 {

        private String endpoint = "";
        private String bucket = "";
        private String region = "auto";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private long uploadUrlExpirationMinutes = 10;
        private long downloadUrlExpirationMinutes = 5;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public long getUploadUrlExpirationMinutes() {
            return uploadUrlExpirationMinutes;
        }

        public void setUploadUrlExpirationMinutes(long uploadUrlExpirationMinutes) {
            this.uploadUrlExpirationMinutes = uploadUrlExpirationMinutes;
        }

        public long getDownloadUrlExpirationMinutes() {
            return downloadUrlExpirationMinutes;
        }

        public void setDownloadUrlExpirationMinutes(long downloadUrlExpirationMinutes) {
            this.downloadUrlExpirationMinutes = downloadUrlExpirationMinutes;
        }
    }
}
