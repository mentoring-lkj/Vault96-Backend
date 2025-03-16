package com.dev.vault96.eventHandler.document.event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
@RequiredArgsConstructor
public class DocumentUploadEvent {
    private final String email;
    private final String fileName;
    private final String documentId;
    private final CompletableFuture<String> futureResult = new CompletableFuture<>();

}
