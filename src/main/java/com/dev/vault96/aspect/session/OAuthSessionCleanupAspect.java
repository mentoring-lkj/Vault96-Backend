package com.dev.vault96.aspect.session;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OAuthSessionCleanupAspect {

    private static final Logger logger = LoggerFactory.getLogger(OAuthSessionCleanupAspect.class);
    private final HttpServletRequest request;

    @Pointcut("execution(* com.dev.vault96.controller.AuthController.googleOAuthLogin(..))")
    public void oauthLoginPointcut() {
    }

    @AfterReturning("oauthLoginPointcut()")
    public void clearSessionAfterOAuthLogin() {
        new SecurityContextLogoutHandler().logout(request, null, SecurityContextHolder.getContext().getAuthentication());
    }

}
