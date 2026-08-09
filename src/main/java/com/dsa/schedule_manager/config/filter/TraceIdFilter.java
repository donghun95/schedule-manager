package com.dsa.schedule_manager.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void  doFilterInternal (
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        try {
            String traceId = UUID.randomUUID()
                    .toString()
                    .replace("-", "");
            MDC.put("traceId", traceId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
