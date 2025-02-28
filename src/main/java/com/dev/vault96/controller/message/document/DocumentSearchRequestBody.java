package com.dev.vault96.controller.message.document;

import com.dev.vault96.entity.document.Tag;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

@Getter
public class DocumentSearchRequestBody {
    Optional<String> name;
    Optional<List<String>> tagIds;
    int page;
    int size;
}
