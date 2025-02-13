package com.dev.vault96.service;


import com.dev.vault96.entity.user.Member;
import com.dev.vault96.repository.member.MemberRepository;
import com.dev.vault96.util.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationProvider authenticationProvider;
    private final MemberRepository memberRepository;
    private final JWTUtils jwtUtils;


    public String extractEmailFromToken(HttpServletRequest request){
        String email = jwtUtils.extractEmailFromRequest(request);
        return email;

    }

    public Member authenticate(String email, String rawPassword) {
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );

        return memberRepository.findMemberByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
