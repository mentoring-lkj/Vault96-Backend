package com.dev.vault96.eventHandler.shared.event;

import com.dev.vault96.entity.shared.SharedDocumentFolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SharedFolderListUpdateEvent {
    private final String owner;
    private final List<SharedDocumentFolder> folders;

}

