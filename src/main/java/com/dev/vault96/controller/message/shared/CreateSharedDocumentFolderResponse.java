package com.dev.vault96.controller.message.shared;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateSharedDocumentFolderResponse {
    private SharedDocumentFolder sharedDocumentFolder;
}
