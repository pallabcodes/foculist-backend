package com.yourorg.platform.foculist.tenancy.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest req && response instanceof HttpServletResponse res) {
            String requestId = req.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            res.setHeader(REQUEST_ID_HEADER, requestId);
            request.setAttribute(REQUEST_ID_MDC_KEY, requestId);
            
            try {
                chain.doFilter(request, response);
            } finally {
                MDC.remove(REQUEST_ID_MDC_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
