package com.dev.vault96.entity.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Language;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@org.springframework.data.mongodb.core.mapping.Document(collection = "document")
@Collation(value="ko_KR")
@Getter
@Setter
@CompoundIndexes({
        @CompoundIndex(name = "owner_name_idx", def = "{'owner': 1, 'name': 1}", unique = true),
        @CompoundIndex(name = "owner_tags_idx", def = "{'owner': 1, 'tags': 1}")
})
public class Document {

    @Id
    private String id;

    private String name;

    @Indexed
    private String owner;

    @DBRef(lazy = true)
    private List<Tag> tags;

    @Indexed
    private List<String> sharedMembers;

    private String format;

    private Date createdAt;

    private long size;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Document document = (Document) obj;
        return Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}