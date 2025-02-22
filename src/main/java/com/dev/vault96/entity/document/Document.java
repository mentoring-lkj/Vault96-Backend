package com.dev.vault96.entity.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@org.springframework.data.mongodb.core.mapping.Document(collection = "document")
@Getter
@Setter
@CompoundIndexes({
        @CompoundIndex(name = "unique_document_owner", def = "{'name': 1, 'owner': 1}", unique = true)
})

public class Document {

    @Id
    private String id;

    private String name;

    private String owner;

    @DBRef(lazy = true)
    private List<Tag> tags;

    private List<String> sharedMembers;

    private String format;

    private Date createdAt;

    private long size;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Document document = (Document) obj;
        return Objects.equals(id, document.id); // ✅ id가 같으면 같은 문서로 간주
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
