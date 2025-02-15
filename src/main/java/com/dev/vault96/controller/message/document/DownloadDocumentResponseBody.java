package com.dev.vault96.controller.message.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadDocumentResponseBody {
    private String presignedDownloadUrl;
}
