package com.dev.vault96.controller.message.document;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;

import java.util.List;

public class DocumentInfo {
    String name;
    List<Tag> tags;

    public DocumentInfo(Document document){
        this.name = document.getName();
        this.tags = document.getTags();
    }
}
