package com.dev.vault96.dto.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

@org.springframework.data.mongodb.core.mapping.Document(collection = "document")
@CompoundIndexes({
        @CompoundIndex(name = "unique_document_owner", def = "{'name': 1, 'owner': 1}", unique = true)
})

public class Document {

    @Id
    private String id;

    private String name;

    private String owner;

}
