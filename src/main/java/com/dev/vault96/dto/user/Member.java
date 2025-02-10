package com.dev.vault96.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;

@Document(collection = "member")
@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Member implements UserDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    @Indexed(unique = true)
    private String nickname;
    
    private PersonName personName;

    private String profileDescription;

    private String profileImageUrl;

    @CreatedDate //
    @Field("createAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createAt;

    private Date deleteAt = null;

    private boolean isValid;

    private int loginCnt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled(){ return this.isValid;}
}
