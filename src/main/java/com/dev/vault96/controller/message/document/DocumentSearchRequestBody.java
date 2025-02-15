package com.dev.vault96.controller.message.document;

import com.dev.vault96.entity.document.Tag;
import lombok.Getter;

import java.util.List;

@Getter
public class DocumentSearchRequestBody {
    String name;
    List<String> tags;
}
