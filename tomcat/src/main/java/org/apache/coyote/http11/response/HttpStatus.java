package org.apache.coyote.http11.response;

import java.util.Arrays;

public enum HttpStatus {

    OK(200, "OK"),
    CREATED(201, "Created"),
    FOUND(302, "Found"),
    UNAUTHORIZED(401, "Unauthorized"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    ;

    private final int statusCode;
    private final String message;

    HttpStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public static HttpStatus from(String code) {
        int statusCode = Integer.parseInt(code);
        return Arrays.stream(values())
                .filter(it -> it.statusCode == statusCode)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

}
