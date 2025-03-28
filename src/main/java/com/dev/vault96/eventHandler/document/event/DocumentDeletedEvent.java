package com.dev.vault96.eventHandler.document.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class DocumentDeletedEvent {
    private final String email;
    private final String documentId;
}
