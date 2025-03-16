package com.dev.vault96.config.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;




@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request
            , @NonNull HttpServletResponse response
            , @NonNull FilterChain filterChain) throws ServletException, IOException {

        logger.debug("JWT FILTER INVOKED");

        if (request.getServletPath().equals("/auth/login") || request.getServletPath().equals("/auth/join")) {
            logger.debug("Skipping JWT filter for login request");
            filterChain.doFilter(request, response);
            return;
        }



        final String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            // invalid header
            logger.debug("header error : " + authHeader);
            filterChain.doFilter(request, response);

        }
        try{
            final String jwt = authHeader.substring(7);
            logger.debug("extracting jwt");
            final String email = jwtService.extractUsername(jwt);
            logger.debug("user Id : " + email);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if(email != null && authentication == null){
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if(jwtService.isTokenValid(jwt, userDetails)){
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails
                            , null
                            , userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                }
                else{
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            }
        }
        catch(Exception exception){
            logger.debug("jwt filter exception");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }

        filterChain.doFilter(request, response);


    }


}