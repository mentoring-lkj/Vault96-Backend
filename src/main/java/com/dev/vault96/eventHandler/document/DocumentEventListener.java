package com.dev.vault96.eventHandler.document;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.eventHandler.document.event.*;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class DocumentEventListener {
    private final S3Service s3Service;
    private final DocumentService documentService;
    private final Logger logger = LoggerFactory.getLogger(DocumentEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void moveDocumentToS3(DocumentUploadEvent event) {
        s3Service.moveFileToDocuments(event.getEmail(), event.getFileName(), event.getDocumentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteDocumentFromS3(DocumentDeletedEvent event) {
        s3Service.deleteDocument(event.getEmail(), event.getDocumentId());
    }
}
