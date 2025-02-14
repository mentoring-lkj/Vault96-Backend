package com.dev.vault96.entity.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

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

    private List<String> tags;

    private List<String> sharedMembers;

    private String format;

    private Date createdAt;
    @Indexed(unique = true)
    private long size;


}
