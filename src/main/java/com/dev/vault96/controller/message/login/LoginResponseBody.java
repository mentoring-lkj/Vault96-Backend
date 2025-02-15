package com.dev.vault96.controller.message.login;

import com.dev.vault96.controller.message.member.MemberInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponseBody {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
    private MemberInfo memberInfo;

}