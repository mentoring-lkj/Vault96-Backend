package com.dev.vault96.controller.message.shared;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GetSharedDocumentFolderDownloadURLResponse {
    private String presignedURL;
}
