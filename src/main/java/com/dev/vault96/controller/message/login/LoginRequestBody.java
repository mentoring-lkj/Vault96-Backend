package com.dev.vault96.controller.message.login;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestBody {
    private String email;
    private String password;
}