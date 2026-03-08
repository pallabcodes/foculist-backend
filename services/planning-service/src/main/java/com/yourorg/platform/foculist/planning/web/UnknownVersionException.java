package com.yourorg.platform.foculist.planning.web;

public class UnknownVersionException extends RuntimeException {
    public UnknownVersionException(String version) {
        super("Unknown response version: " + version);
    }
}
