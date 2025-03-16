package com.dev.vault96.eventHandler.shared;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.eventHandler.shared.event.*;
import com.dev.vault96.service.s3.S3Service;
import com.dev.vault96.service.shared.SharedDocumentFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SharedFolderEventListener {
    private final S3Service s3Service;
    private final SharedDocumentFolderService sharedDocumentFolderService;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void uploadFolderToS3(SharedFolderCreatedEvent event) throws IOException{
        try {
            List<byte[]> documentContents = event.getDocuments().stream()
                    .map(doc -> s3Service.getDocumentContent(event.getOwner(), doc.getId()))
                    .collect(Collectors.toList());

            List<String> fileNames = event.getDocuments().stream().map(Document::getName).collect(Collectors.toList());

            String s3Url = s3Service.uploadFolder(event.getOwner(), event.getFolderId(), documentContents, fileNames);
        }
        catch(IOException e){
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteFolderFromS3(SharedFolderDeletedEvent event) {
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void updateFolder(SharedFolderUpdateEvent event){
        sharedDocumentFolderService.updateSharedFolder(event.getFolder(), event.getOwner(), event.getNewDocuments());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void updateFolders(SharedFolderListUpdateEvent event){
        List<SharedDocumentFolder> folders = event.getFolders();
        for(SharedDocumentFolder folder : folders){
            sharedDocumentFolderService.updateSharedFolder(folder, folder.getOwner(), folder.getDocuments());
        }
        sharedDocumentFolderService.cleanupSharedFolders(event.getOwner(), folders);

    }
}
