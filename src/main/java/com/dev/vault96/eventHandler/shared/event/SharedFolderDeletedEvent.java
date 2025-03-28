package com.dev.vault96.eventHandler.shared.event;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SharedFolderDeletedEvent {
    private final String owner;
    private final List<SharedDocumentFolder> folders;

}

