package com.dev.vault96.service.shared;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.eventHandler.shared.event.SharedFolderCreatedEvent;
import com.dev.vault96.repository.document.DocumentRepository;
import com.dev.vault96.repository.shared.SharedDocumentFolderRepository;
import com.dev.vault96.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedDocumentFolderService {

    private final SharedDocumentFolderRepository sharedDocumentFolderRepository;
    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher applicationEventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(SharedDocumentFolderService.class);
    public SharedDocumentFolder findSharedDocumentFolderById(String id) {
        return sharedDocumentFolderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다: " + id));
    }

    public List<SharedDocumentFolder> findSharedDocumentFoldersByOwner(String email) {
        return sharedDocumentFolderRepository.findSharedDocumentFoldersByOwner(email);
    }

    public SharedDocumentFolder findSharedDocumentFolderByIdAndOwner(String id, String email) {
        return sharedDocumentFolderRepository.findSharedDocumentFolderByIdAndOwner(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 사용자의 폴더를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public SharedDocumentFolder createSharedFolder(String owner, String name, List<String> documentIds) throws IOException {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "폴더 이름은 필수입니다.");
        }
        if (documentIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문서 목록이 비어 있습니다.");
        }
        if(sharedDocumentFolderRepository.findSharedDocumentFolderByOwnerAndName(owner, name+"zip").isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 폴더 이름입니다.");
        }
        List<Document> validDocuments = documentRepository.findAllById(documentIds);
        List<String> invalidDocs = validDocuments.stream()
                .filter(doc -> !doc.getOwner().equals(owner))
                .map(Document::getId)
                .collect(Collectors.toList());

        if (!invalidDocs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다음 문서에 대한 권한이 없습니다: " + invalidDocs);
        }

        String folderId = UUID.randomUUID().toString();

        SharedDocumentFolder sharedFolder = SharedDocumentFolder.builder()
                .uuid(folderId)
                .owner(owner)
                .name(name)
                .documents(validDocuments)
                .expiredAt(LocalDateTime.now().plusDays(2))
                .build();
        SharedDocumentFolder folder = sharedDocumentFolderRepository.save(sharedFolder);
        applicationEventPublisher.publishEvent(new SharedFolderCreatedEvent(folder.getOwner(), folder.getUuid(), folder.getDocuments()));
        return folder;
    }

    public void updateSharedFolder(SharedDocumentFolder folder, String owner, List<Document> newDocuments){
        if (!StringUtils.hasText(folder.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "폴더 이름은 필수입니다.");
        }
        List<Document> validDocuments = documentRepository.findAllById(newDocuments.stream().map(Document::getId).collect(Collectors.toList()));

        if (validDocuments.size() != newDocuments.size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없는 문서에 접근하였습니다" );
        }

        List<byte[]> documentContents = validDocuments.stream()
                .map(doc -> s3Service.getDocumentContent(owner, doc.getId()))
                .collect(Collectors.toList());

        List<String> fileNames = validDocuments.stream().map(Document::getName).collect(Collectors.toList());

        try {
            s3Service.deleteDocument(owner, folder.getUuid());
            s3Service.uploadFolder(owner, folder.getUuid(), documentContents, fileNames);
        }catch(IOException e){
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public String getPresignedDownloadURL(String owner, String fileName, String id) {
        return s3Service.getSharedDocumentFolderPresignedDownloadUrl(owner, fileName, id).toString();
    }

    @Transactional
    public boolean deleteSharedDocument(String owner, String id) {
        SharedDocumentFolder folder = sharedDocumentFolderRepository.findSharedDocumentFolderByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 폴더를 찾을 수 없습니다: " + id));
        String uuid = folder.getUuid();
        s3Service.deleteSharedDocumentFolder(owner, uuid);
        sharedDocumentFolderRepository.delete(folder);
        return true;
    }

    @Transactional
    public void cleanupSharedFolders(String email, List<SharedDocumentFolder> folders) {
        List<String> emptyFolderIds = folders.stream()
                .filter(folder -> folder.getDocuments().isEmpty())
                .map(SharedDocumentFolder::getId)
                .collect(Collectors.toList());

        if (!emptyFolderIds.isEmpty()) {
            logger.debug("Deleting {} empty folders", emptyFolderIds.size());
            sharedDocumentFolderRepository.deleteAllByIdIn(emptyFolderIds);
        } else {
            logger.debug("No empty folders found.");
        }

        List<SharedDocumentFolder> nonEmptyFolders = folders.stream()
                .filter(folder -> !folder.getDocuments().isEmpty())
                .collect(Collectors.toList());

        sharedDocumentFolderRepository.saveAll(nonEmptyFolders);

    }

    public List<SharedDocumentFolder> findByOwnerAndDocumentsContaining(String owner, Document document) {
        return sharedDocumentFolderRepository.findByOwnerAndDocumentsContaining(owner, document);
    }

    public void save(SharedDocumentFolder folder) {
        sharedDocumentFolderRepository.save(folder);
    }
}
