package com.dev.vault96.controller.message;

import com.dev.vault96.dto.user.PersonName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberJoinForm {
    private String email;
    private String nickname;
    private String password;
    private PersonName personName;
    private String profileDescription;
    private String profileImageUrl;

}
