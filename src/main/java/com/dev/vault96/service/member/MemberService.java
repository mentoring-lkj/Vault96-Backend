package com.dev.vault96.service.member;

import com.dev.vault96.controller.message.member.MemberJoinForm;
import com.dev.vault96.entity.user.Member;
import com.dev.vault96.repository.member.MemberRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public void save(Member member) throws DuplicateKeyException{

        try{memberRepository.save(member);}
        catch(DuplicateKeyException e){throw e;}
    }
}
