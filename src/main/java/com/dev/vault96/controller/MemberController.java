package com.dev.vault96.controller;

import com.dev.vault96.controller.message.MemberInfo;
import com.dev.vault96.dto.user.Member;
import com.dev.vault96.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;

    @GetMapping("/whoami")
    public ResponseEntity<MemberInfo> getWhoAmI() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(403).body(null);
        }

        String email = authentication.getName();
        Member member = memberRepository.findMemberByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new MemberInfo(member));
    }
}
