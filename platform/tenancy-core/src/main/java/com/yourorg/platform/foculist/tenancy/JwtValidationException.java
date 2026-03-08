package com.yourorg.platform.foculist.tenancy;

class JwtValidationException extends RuntimeException {
    JwtValidationException(String message) {
        super(message);
    }
}
