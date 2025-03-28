package com.dev.vault96.entity.shared;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "shared_document_folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedDocumentFolder {
    @Id
    private String id;

    @Indexed(unique = true)
    private String uuid;

    private String owner;

    private String name;

    @DBRef(lazy = true)
    private List<com.dev.vault96.entity.document.Document> documents;

    @Indexed(expireAfter = "0")
    private LocalDateTime expiredAt;
}

