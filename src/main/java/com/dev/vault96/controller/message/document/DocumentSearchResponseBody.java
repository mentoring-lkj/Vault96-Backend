package com.dev.vault96.controller.message.document;

import com.dev.vault96.entity.document.Document;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DocumentSearchResponseBody {
    private List<Document> files;
    private boolean hasNext;
    private int totalCount;
}
