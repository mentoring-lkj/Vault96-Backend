package com.dev.vault96.controller.message.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Optional;

@Getter
@NoArgsConstructor // JSON 역직렬화 시 기본 생성자 필요
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON 직렬화 시 null 값 제외
public class UpdateDocumentRequestBody {
    private Optional<String> name = Optional.empty();
    private Optional<List<String>> tagIds = Optional.empty();

    // ✅ JSON 역직렬화 시 `null`이 전달될 경우 Optional.empty()로 설정
    public UpdateDocumentRequestBody(Optional<String> name, Optional<List<String>> tagIds) {
        this.name = name != null ? name : Optional.empty();
        this.tagIds = tagIds != null ? tagIds : Optional.empty();
    }
}
