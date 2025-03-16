package com.dev.vault96.eventHandler.shared.event;

import com.dev.vault96.entity.document.Document;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SharedFolderCreatedEvent {
    private final String owner;
    private final String folderId;
    private final List<Document> documents;
}