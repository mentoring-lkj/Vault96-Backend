package com.dev.vault96.service.document;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.repository.document.DocumentRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final MongoTemplate mongoTemplate;

    // ✅ 문서 ID로 조회
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

    public List<Document> getDocumentsByIdsAndOwner(List<String> ids, String owner) {
        // 1 ID 리스트를 기반으로 문서 조회
        List<Document> foundDocuments = documentRepository.findAllByIdIn(ids);

        // 2 조회된 Document의 ID 및 Owner 정보 추출
        Set<String> foundIds = foundDocuments.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());

        Set<String> foundOwners = foundDocuments.stream()
                .map(Document::getOwner)
                .collect(Collectors.toSet());

        // 3 요청한 ID 리스트 중 존재하지 않는 ID 찾기
        List<String> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        // 4 요청한 Owner와 일치하지 않는 Document 찾기
        List<String> invalidOwners = foundDocuments.stream()
                .filter(doc -> !doc.getOwner().equals(owner))
                .map(Document::getId)
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "존재하지 않는 Document ID: " + missingIds);
        }

        // 6️⃣ Owner 불일치 문서가 있다면 요청 거부 (403 Forbidden)
        if (!invalidOwners.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "해당 Document의 소유자가 아닙니다: " + invalidOwners);
        }

        return foundDocuments;
    }


}
