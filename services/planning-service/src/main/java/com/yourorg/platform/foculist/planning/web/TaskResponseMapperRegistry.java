package com.yourorg.platform.foculist.planning.web;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TaskResponseMapperRegistry {
    private final Map<String, TaskResponseMapper> mappers;
    private static final String DEFAULT_VERSION = "V1";

    public TaskResponseMapperRegistry(Map<String, TaskResponseMapper> mappers) {
        this.mappers = mappers;
    }

    public TaskResponseMapper resolve(String versionHeader) {
        String versionKey = (versionHeader == null || versionHeader.isBlank())
                ? DEFAULT_VERSION
                : versionHeader.toUpperCase();
        TaskResponseMapper mapper = mappers.get("taskResponseMapper" + versionKey);
        if (mapper == null) {
            throw new UnknownVersionException(versionHeader);
        }
        return mapper;
    }
}
