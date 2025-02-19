package com.dev.vault96.controller.message.document;

import com.dev.vault96.entity.document.Tag;
import lombok.Getter;

import java.util.List;

@Getter
public class EditDocumentRequestBody {
    String name;
    String newName;
    List<Tag> tags;
}

