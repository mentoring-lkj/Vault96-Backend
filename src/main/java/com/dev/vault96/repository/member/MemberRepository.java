package com.dev.vault96.repository.member;

import com.dev.vault96.entity.user.Member;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MemberRepository extends MongoRepository<Member, String> {
    Optional<Member> findMemberByEmail(String email);

}
