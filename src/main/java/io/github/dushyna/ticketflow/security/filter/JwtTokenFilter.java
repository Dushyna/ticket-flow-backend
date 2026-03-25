package io.github.dushyna.ticketflow.security.filter;

import io.github.dushyna.ticketflow.security.service.CustomUserDetailsService;
import io.github.dushyna.ticketflow.security.service.JwtTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

import static io.github.dushyna.ticketflow.security.constants.Constants.ACCESS_TOKEN_COOKIE;

@Component
@RequiredArgsConstructor

public class JwtTokenFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(JwtTokenFilter.class);

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        final JwtTokenService.TokenType tokenType = JwtTokenService.TokenType.ACCESS;

        if (StringUtils.isNoneBlank(token)) {
            try {
                if (jwtTokenService.validateToken(token, tokenType)) {
                    final String username = jwtTokenService.getUsernameFromToken(token, tokenType);
                    try{
                    final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                } catch (Exception ex){
                        SecurityContextHolder.clearContext();
                    logger.warn("JWT token is valid but user not found in DB: {}", username); }
                    }
            } catch (ExpiredJwtException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header or cookie.
     */
    private String resolveToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, ACCESS_TOKEN_COOKIE);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }
}
