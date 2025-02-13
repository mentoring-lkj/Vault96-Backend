package com.dev.vault96.entity.user;

import lombok.Getter;

@Getter
public class PersonName {

    private String firstName;

    private String middleName;

    private String lastName;

    // MongoDB를 위한 기본 생성자 (반드시 필요)
    protected PersonName() {
    }

    public PersonName(String firstName, String middleName, String lastName) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
    }
}
