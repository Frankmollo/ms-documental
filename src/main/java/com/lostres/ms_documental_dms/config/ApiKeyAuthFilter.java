package com.lostres.ms_documental_dms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final DmsProperties dmsProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isSecurityEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-Api-Key");
        if (apiKey == null || !apiKey.equals(dmsProperties.getSecurity().getApiKey())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"API key inválida o faltante\"}");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("api-client", null, List.of())
        );
        filterChain.doFilter(request, response);
    }

    private boolean isSecurityEnabled() {
        boolean hasApiKey = dmsProperties.getSecurity().getApiKey() != null
                && !dmsProperties.getSecurity().getApiKey().isBlank();
        return hasApiKey && (dmsProperties.getSecurity().isEnabled()
                || dmsProperties.getSecurity().isEnforceApiKey());
    }
}
