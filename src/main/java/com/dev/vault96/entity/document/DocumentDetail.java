package com.dev.vault96.entity.document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "documentDetail")
@Getter
@Setter
@AllArgsConstructor
public class DocumentDetail {
    @Id
    private String id;
    @Indexed(unique = true)
    private String documentId;
    @Indexed(unique = true)
    private int version;
    private LocalDateTime createdAt;
    @Indexed(unique = true)
    private String s3Url;
    private long size;

    public DocumentDetail() {}

}
