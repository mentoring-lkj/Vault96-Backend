package com.dev.vault96.entity.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "tag")
public class Tag {
    @Id
    private String id;
    private String owner;
    private String name;

    @JsonCreator
    public Tag(@JsonProperty("id") String id,
               @JsonProperty("owner") String owner,
               @JsonProperty("name") String name) {
        this.id = id;
        this.owner = owner;
        this.name = name;
    }

    // 기본 생성자 (직렬화 및 MongoDB 호환성 위해 필요)
    public Tag() {}
}
