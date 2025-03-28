package com.dev.vault96.service.document;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.eventHandler.document.event.DocumentDeletedEvent;
import com.dev.vault96.eventHandler.document.event.DocumentUploadEvent;
import com.dev.vault96.eventHandler.shared.event.SharedFolderListUpdateEvent;
import com.dev.vault96.repository.document.DocumentRepository;
import com.dev.vault96.service.s3.S3Service;
import com.dev.vault96.service.shared.SharedDocumentFolderService;
import com.dev.vault96.util.DocumentUtil;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableTransactionManagement
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final MongoTemplate mongoTemplate;
    private final SharedDocumentFolderService sharedDocumentFolderService;
    private final DocumentUtil documentUtil;
    private final S3Service s3Service;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApplicationContext applicationContext;
    private final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    public Document findDocumentById(String id) {
        return documentRepository.findDocumentById(id).orElse(null);
    }

    public List<Document> findAllByIdIn(List<String> ids){
        return documentRepository.findAllByIdIn(ids);
    }

    public List<Document> findDocumentsByOwner(String owner){
        return documentRepository.findDocumentsByOwner(owner);
    }
    public Document findDocumentByOwnerAndName(String owner, String name){
        Optional<Document> document = documentRepository.findDocumentByOwnerAndName(owner, name);
        if(document.isPresent()){
            return document.get();
        }
        return null;
    }


    public List<Document> validateAndGetDocumentsByOwner(List<String> ids, String owner) {
        List<Document> foundDocuments = documentRepository.findAllByIdIn(ids);

        Set<String> foundIds = foundDocuments.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());

        // 존재하지 않는 ID 필터링
        List<String> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "존재하지 않는 Document ID: " + missingIds);
        }

        // owner 불일치 검증
        List<String> invalidOwners = foundDocuments.stream()
                .filter(doc -> !doc.getOwner().equals(owner))
                .map(Document::getId)
                .collect(Collectors.toList());

        if (!invalidOwners.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "해당 Document의 소유자가 아닙니다: " + invalidOwners);
        }

        return foundDocuments;
    }

    public Page<Document> findAllDocuments(String owner, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findAllByOwner(owner, pageable);
    }

    public long countAllDocuments(String owner) {
        return documentRepository.countByOwner(owner);
    }

    public Page<Document> findDocumentPageByOwnerAndNameLike(String owner, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findDocumentsByOwnerAndNameLike(owner, name, pageable);
    }
    public long countDocumentsByOwnerAndName(String owner, String name) {
        return documentRepository.countByOwnerAndNameContaining(owner, name);
    }

    public Page<Document> searchDocuments(String owner, String name, List<String> tagIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.searchDocuments(owner, name, tagIds, pageable);
    }

    public long countDocumentsByOwnerAndTags(String owner, List<String> tagIds) {
        return documentRepository.countByOwnerAndTags(owner, tagIds);
    }

    public long countDocumentsByOwnerAndNameAndTags(String owner, String name, List<String> tagIds) {
        return documentRepository.countByOwnerAndNameAndTags(owner, name, tagIds);
    }

    public void updateDocumentName(String documentId, String newName) {
        documentRepository.findDocumentById(documentId).ifPresent(document -> {
            document.setName(newName);
            documentRepository.save(document);
        });
    }

    public boolean addTagToDocument(String documentId, Tag tag) {
        Query query = new Query(Criteria.where("id").is(documentId));
        Update update = new Update().addToSet("tags", tag);
        return mongoTemplate.updateFirst(query, update, Document.class).getModifiedCount() > 0;
    }

    public List<Document> findDocumentsBySharedMember(String sharedMember) {
        return documentRepository.findDocumentsBySharedMembersContaining(sharedMember);
    }

    public void save(Document document) throws DuplicateKeyException {
        try {
            documentRepository.save(document);
        } catch (DuplicateKeyException e) {
            throw e;
        }
    }

    public void deleteDocument(Document document) {
        documentRepository.delete(document);
    }


    @Transactional
    public boolean deleteDocument(String email, String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));
        List<SharedDocumentFolder> folders = null;

        if (!document.getOwner().equals(email)) {
            throw new SecurityException("해당 문서의 소유자가 아닙니다: " + documentId);
        }
        try{
            folders = sharedDocumentFolderService.findByOwnerAndDocumentsContaining(email, document);

            for (SharedDocumentFolder folder : folders) {
                folder.getDocuments().remove(document);
                sharedDocumentFolderService.save(folder);
            }

            documentRepository.delete(document);
            applicationEventPublisher.publishEvent(new DocumentDeletedEvent(document.getOwner(), document.getId()));
            applicationEventPublisher.publishEvent(new SharedFolderListUpdateEvent(document.getOwner(), folders));
            return true;

        }catch(ResponseStatusException e){
            e.printStackTrace();
            return false;
        }

    }


    public List<Document> getDocumentsByIdsAndOwner(List<String> ids, String owner) {
        List<Document> foundDocuments = documentRepository.findAllByIdIn(ids);

        Set<String> foundIds = foundDocuments.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());

        Set<String> foundOwners = foundDocuments.stream()
                .map(Document::getOwner)
                .collect(Collectors.toSet());

        List<String> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        List<String> invalidOwners = foundDocuments.stream()
                .filter(doc -> !doc.getOwner().equals(owner))
                .map(Document::getId)
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "존재하지 않는 Document ID: " + missingIds);
        }

        if (!invalidOwners.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "해당 Document의 소유자가 아닙니다: " + invalidOwners);
        }

        return foundDocuments;
    }

    @Transactional
    public void createDocument(String email, String fileName, String fileNameNFC) {
        Document document = new Document();
        document.setName(fileNameNFC);
        document.setOwner(email);
        document.setFormat(documentUtil.getFileExtension(fileNameNFC));
        document.setCreatedAt(new Date());
        document.setTags(new ArrayList<>());
        document.setSharedMembers(new ArrayList<>());
        document.setSize(s3Service.getTempFileSize(email, fileName));
        try{
            Document savedDocument = documentRepository.save(document);
            applicationEventPublisher.publishEvent(new DocumentUploadEvent(email, fileName, savedDocument.getId()));
        }catch(DuplicateKeyException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        }
    }


}
