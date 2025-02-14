package com.dev.vault96.controller.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDocumentResponseBody {
    private String presignedUploadUrl;
}
