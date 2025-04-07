package com.dev.vault96.controller.message.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Optional;

@Getter
public class UpdateDocumentRequestBody {
    private String name;
    private List<String> tagIds;
}
