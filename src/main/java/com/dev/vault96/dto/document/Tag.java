package com.dev.vault96.dto.document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tag")
@Getter
@Setter
@AllArgsConstructor
public class Tag {
    @Id
    private String id;
    private String userId;

    @Indexed(unique = true)
    private String tagId;
    @Indexed(unique = true)
    private String name;

    public Tag() {}

}

