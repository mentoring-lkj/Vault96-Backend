package com.dev.vault96.entity.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tag")
@CompoundIndex(name = "userId_name_unique_idx", def = "{'userId': 1, 'name': 1}", unique = true) // ✅ 복합 인덱스 추가
@Getter
@Setter
@AllArgsConstructor
public class Tag {
    @Id
    private String id;
    private String owner;
    private String name;

    public Tag() {}
}
