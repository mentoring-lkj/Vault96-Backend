package com.dev.vault96.controller.message.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestBody {
    @NotNull
    @Email
    private String email;
    @NotNull
    @Size(min = 4, message = "비밀번호는 최소 6자리 이상이어야 합니다.")
    private String password;
}