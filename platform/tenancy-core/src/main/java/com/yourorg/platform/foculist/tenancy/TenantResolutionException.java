package com.yourorg.platform.foculist.tenancy;

public class TenantResolutionException extends RuntimeException {
    private final int status;

    public TenantResolutionException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
