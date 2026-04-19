package com.yourorg.platform.foculist.tenancy.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogMaskingConverter extends ClassicConverter {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w-.]+@)([\\w-.]+\\.[\\w-]{2,4})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password=|passwd=|secret=)([^\\s&,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(Bearer|token|apiKey)[:\\s]+([\\w-]+\\.[\\w-]+\\.[\\w-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        
        // 1. Mask Emails: user@example.com -> ****@example.com
        Matcher emailMatcher = EMAIL_PATTERN.matcher(message);
        while (emailMatcher.find()) {
            message = message.replace(emailMatcher.group(1), "****@");
        }
        
        // 2. Mask Passwords/Secrets: password=12345 -> password=****
        Matcher passwordMatcher = PASSWORD_PATTERN.matcher(message);
        while (passwordMatcher.find()) {
            message = message.replace(passwordMatcher.group(2), "****");
        }
        
        // 3. Mask Tokens/JWTs: Bearer abc.123.xyz -> Bearer ****
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(message);
        while (tokenMatcher.find()) {
            message = message.replace(tokenMatcher.group(2), "****");
        }
        
        return message;
    }
}
