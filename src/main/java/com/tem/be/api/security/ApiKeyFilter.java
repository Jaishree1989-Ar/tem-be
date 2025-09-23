package com.tem.be.api.security;

import com.tem.be.api.utils.RestConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to check API key in request headers.
 * Allows only requests with valid API key except for Swagger and public endpoints.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    /**
     * API key value from application properties.
     */
    @Value("${api.key}")
    private String apiKey;

    /**
     * Filters incoming requests and validates the API key.
     *
     * @param request      the HTTP request
     * @param response     the HTTP response
     * @param filterChain  the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.startsWith("/cos-tem-api-docs") ||
                path.startsWith("/cos-tem-api") ||
                path.startsWith("/actuator")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader("API_KEY");
        if (requestApiKey == null) {
            requestApiKey = request.getHeader("api_key");
        }
        if (apiKey.equals(requestApiKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(RestConstants.UNAUTHORIZED_STRING);
        }
    }
}
