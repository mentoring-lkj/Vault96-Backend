package com.dev.vault96.controller.message;

import com.dev.vault96.dto.user.Member;
import com.dev.vault96.dto.user.PersonName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class MemberInfo {
    private String email;
    private String nickname;
    private PersonName personName;
    private Date createAt;
    private String profileDescription;
    private String profileImageUrl;
    public MemberInfo(Member member){
        this.email = member.getEmail();
        this.nickname = member.getNickname();
        this.personName = member.getPersonName();
        this.createAt = member.getCreateAt();
        this.profileDescription = member.getProfileDescription();
        this.profileImageUrl = member.getProfileImageUrl();
    }
}
