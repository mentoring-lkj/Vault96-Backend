package com.dev.vault96.service.member;

import com.dev.vault96.controller.AuthController;
import com.dev.vault96.controller.message.MemberInfo;
import com.dev.vault96.controller.message.MemberJoinForm;
import com.dev.vault96.dto.user.Member;
import com.dev.vault96.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);

    public Member findMemberByEmail(String email){
        Optional<Member> member = memberRepository.findMemberByEmail(email);
        if(member.isPresent()) return member.get();
        else return null;
    }

    public Member insertMember(MemberJoinForm memberJoinForm){
        if (memberRepository.findMemberByEmail(memberJoinForm.getEmail()).isPresent()) {
            logger.warn("Registration failed: Email already exists");
            return null;
        }

        Member newMember = new Member();
        newMember.setEmail(memberJoinForm.getEmail());
        newMember.setPassword(passwordEncoder.encode(memberJoinForm.getPassword()));
        newMember.setNickname(memberJoinForm.getNickname());
        newMember.setPersonName(memberJoinForm.getPersonName());
        newMember.setProfileDescription(memberJoinForm.getProfileDescription());
        newMember.setProfileImageUrl(memberJoinForm.getProfileImageUrl());
        newMember.setValid(true);

        Member savedMember = memberRepository.save(newMember);
        if (savedMember == null) {
            logger.warn("Registration failed: Unable to save user");
            return null;
        }

        logger.info("Registration successful: " + newMember.getEmail());
        return savedMember;

    }

    public void save(Member member){
        memberRepository.save(member);
    }
}
