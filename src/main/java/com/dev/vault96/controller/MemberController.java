package com.dev.vault96.controller;

import com.dev.vault96.controller.message.member.MemberInfo;
import com.dev.vault96.controller.message.member.MemberJoinForm;
import com.dev.vault96.entity.user.Member;
import com.dev.vault96.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    @GetMapping("/whoami")
    public ResponseEntity<MemberInfo> getWhoAmI() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(403).body(null);
        }

        String email = authentication.getName();
        Member member = memberService.findMemberByEmail(email);
        return ResponseEntity.ok(new MemberInfo(member));
    }
    @PostMapping("/join")
    public ResponseEntity<MemberInfo> joinMember(@RequestBody MemberJoinForm memberJoinForm) {
        Member isSuccess = memberService.insertMember(memberJoinForm);
        if (isSuccess==null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        else {return ResponseEntity.ok(new MemberInfo(isSuccess));}
    }
    
}
