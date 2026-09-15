package com.chacha.multitenantsaas.whiteboards;

public class WhiteboardVersionConflictException extends RuntimeException {

    private final long expectedVersion;
    private final long currentVersion;

    public WhiteboardVersionConflictException(long expectedVersion, long currentVersion) {
        super(
                "Whiteboard version conflict: expected "
                        + expectedVersion
                        + " but current version is "
                        + currentVersion);
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }
}
