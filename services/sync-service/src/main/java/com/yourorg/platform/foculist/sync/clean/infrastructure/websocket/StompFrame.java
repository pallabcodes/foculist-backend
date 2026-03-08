package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import java.util.LinkedHashMap;
import java.util.Map;

public record StompFrame(
        String command,
        Map<String, String> headers,
        String body
) {
    public static StompFrame parse(String rawFrame) {
        String frame = rawFrame.endsWith("\u0000") ? rawFrame.substring(0, rawFrame.length() - 1) : rawFrame;
        String[] sections = frame.split("\n\n", 2);
        String[] headerLines = sections[0].split("\n");
        String command = headerLines[0].trim();
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < headerLines.length; i++) {
            String headerLine = headerLines[i];
            int separator = headerLine.indexOf(':');
            if (separator > 0) {
                headers.put(headerLine.substring(0, separator), headerLine.substring(separator + 1));
            }
        }
        String body = sections.length > 1 ? sections[1] : "";
        return new StompFrame(command, headers, body);
    }

    public String encode() {
        StringBuilder builder = new StringBuilder(command).append('\n');
        headers.forEach((key, value) -> builder.append(key).append(':').append(value).append('\n'));
        builder.append('\n');
        if (body != null) {
            builder.append(body);
        }
        builder.append('\u0000');
        return builder.toString();
    }
}
