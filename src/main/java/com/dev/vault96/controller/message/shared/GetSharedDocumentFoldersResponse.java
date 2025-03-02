package com.dev.vault96.controller.message.shared;

import com.dev.vault96.entity.shared.SharedDocumentFolder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetSharedDocumentFoldersResponse {
    List<SharedDocumentFolder>  sharedDocumentFolders;
}
