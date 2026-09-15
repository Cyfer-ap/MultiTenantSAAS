package com.chacha.multitenantsaas.whiteboards;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WhiteboardVersionConflictException extends RuntimeException {

    public WhiteboardVersionConflictException(long expectedVersion, long currentVersion) {
        super(
                "Whiteboard version conflict: expected "
                        + expectedVersion
                        + " but current version is "
                        + currentVersion);
    }
}
