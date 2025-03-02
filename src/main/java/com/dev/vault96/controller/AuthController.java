package com.dev.vault96.controller;

import com.dev.vault96.config.security.jwt.JWTService;
import com.dev.vault96.controller.message.login.LoginRequestBody;
import com.dev.vault96.controller.message.login.LoginResponseBody;
import com.dev.vault96.controller.message.member.MemberInfo;
import com.dev.vault96.entity.user.Member;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            if(member.getLoginCnt() > 5){
                member.setValid(false);
            }
            member.setLoginCnt(member.getLoginCnt()+1);
            memberService.save(member);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @GetMapping("/token/refresh")
    public ResponseEntity<LoginResponseBody> refreshTokenHandler() {
        return null;
    }
}
