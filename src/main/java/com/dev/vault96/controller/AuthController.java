package com.dev.vault96.controller;

import com.dev.vault96.config.security.jwt.JWTService;
import com.dev.vault96.controller.message.login.LoginRequestBody;
import com.dev.vault96.controller.message.login.LoginResponseBody;
import com.dev.vault96.controller.message.member.MemberInfo;
import com.dev.vault96.entity.user.Member;
import com.dev.vault96.entity.user.PersonName;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final JWTService jwtService;
    private final MemberService memberService;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<LoginResponseBody> postLogin(@RequestBody LoginRequestBody loginRequestBody) {
        logger.info("Login attempt: " + loginRequestBody.getEmail());

        try {
            Member member = authService.authenticate(loginRequestBody.getEmail(), loginRequestBody.getPassword());

            String jwtAccessToken = jwtService.generateToken(member);
            String jwtRefreshToken = jwtService.generateRefreshToken(member);

            LoginResponseBody loginResponseBody = new LoginResponseBody();
            loginResponseBody.setAccessToken(jwtAccessToken);
            loginResponseBody.setRefreshToken(jwtRefreshToken);
            loginResponseBody.setAccessTokenExpiresIn(3600000);
            loginResponseBody.setRefreshTokenExpiresIn(604800000);
            loginResponseBody.setMemberInfo(new MemberInfo(member));

            return ResponseEntity.ok(loginResponseBody);
        } catch (Exception e) {
            logger.warn("Login failed: Invalid credentials");
            Member member = memberService.findMemberByEmail(loginRequestBody.getEmail());
            if(member != null){
                if(member.getLoginCnt() > 5){
                    member.setValid(false);
                }
            }
            member.setLoginCnt(member.getLoginCnt()+1);
            memberService.save(member);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @GetMapping("/login/oauth")
    public ResponseEntity<LoginResponseBody> googleOAuthLogin(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        HttpSession session = request.getSession();
        if (session.getAttribute("oauth_login_executed") != null) {
            logger.warn("⚠ Duplicate OAuth login request detected, ignoring...");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null);
        }
        session.setAttribute("oauth_login_executed", true);

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        Map<String, Object> attributes = oauthUser.getAttributes();

        String email = (String) attributes.get("email");

        Member member = memberService.findMemberByEmail(email);
        if (member == null) {
            member = new Member();
            member.setEmail(email);
            member.setPersonName(new PersonName((String) attributes.get("given_name"), "", (String) attributes.get("family_name")));
            member.setProfileImageUrl((String) attributes.get("picture"));
            member.setCreateAt(new Date());
            member.setValid(true);
            member.setNickname((String)attributes.get("name"));
            member.setProfileDescription("");
            memberService.save(member);
        }

        String jwtAccessToken = jwtService.generateToken(member);
        String jwtRefreshToken = jwtService.generateRefreshToken(member);

        LoginResponseBody loginResponseBody = new LoginResponseBody();
        loginResponseBody.setAccessToken(jwtAccessToken);
        loginResponseBody.setRefreshToken(jwtRefreshToken);
        loginResponseBody.setAccessTokenExpiresIn(3600000);
        loginResponseBody.setRefreshTokenExpiresIn(604800000);
        loginResponseBody.setMemberInfo(new MemberInfo(member));

        return ResponseEntity.ok(loginResponseBody);
    }


    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/logout")
    public void logout(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

    }



    @GetMapping("/token/refresh")
    public ResponseEntity<LoginResponseBody> refreshTokenHandler() {
        return null;
    }
}
