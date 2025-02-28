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
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final MongoTemplate mongoTemplate;

    // ✅ 문서 ID로 조회
    public Document findDocumentById(String id) {
        return documentRepository.findDocumentById(id).orElse(null);
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

    // ✅ 특정 사용자의 모든 문서 조회 (페이징 적용)
    public Page<Document> findAllDocuments(String owner, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findAllByOwner(owner, pageable);
    }

    // ✅ 특정 사용자의 모든 문서 개수 조회
    public long countAllDocuments(String owner) {
        return documentRepository.countByOwner(owner);
    }

    // ✅ 이름 포함 검색 (페이징)
    public Page<Document> findDocumentPageByOwnerAndNameLike(String owner, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findDocumentsByOwnerAndNameLike(owner, name, pageable);
    }
    // ✅ 특정 이름을 포함하는 문서 개수 조회
    public long countDocumentsByOwnerAndName(String owner, String name) {
        return documentRepository.countByOwnerAndNameContaining(owner, name);
    }

    // ✅ 태그를 포함하는 문서 검색
    public Page<Document> searchDocuments(String owner, String name, List<String> tagIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.searchDocuments(owner, name, tagIds, pageable);
    }

    // ✅ 특정 태그를 포함하는 문서 개수 조회
    public long countDocumentsByOwnerAndTags(String owner, List<String> tagIds) {
        return documentRepository.countByOwnerAndTags(owner, tagIds);
    }

    // ✅ 이름과 태그를 동시에 만족하는 문서 개수 조회
    public long countDocumentsByOwnerAndNameAndTags(String owner, String name, List<String> tagIds) {
        return documentRepository.countByOwnerAndNameAndTags(owner, name, tagIds);
    }

    // ✅ 문서 이름 변경
    public void updateDocumentName(String documentId, String newName) {
        documentRepository.findDocumentById(documentId).ifPresent(document -> {
            document.setName(newName);
            documentRepository.save(document);
        });
    }

    // ✅ 문서에 태그 추가 (중복 없이)
    public boolean addTagToDocument(String documentId, Tag tag) {
        Query query = new Query(Criteria.where("id").is(documentId));
        Update update = new Update().addToSet("tags", tag);
        return mongoTemplate.updateFirst(query, update, Document.class).getModifiedCount() > 0;
    }

    // ✅ 공유 문서 조회
    public List<Document> findDocumentsBySharedMember(String sharedMember) {
        return documentRepository.findDocumentsBySharedMembersContaining(sharedMember);
    }

    // ✅ 문서 저장 (중복 검사 포함)
    public void save(Document document) throws DuplicateKeyException {
        try {
            documentRepository.save(document);
        } catch (DuplicateKeyException e) {
            throw e;
        }
    }

    // ✅ 문서 삭제
    public void deleteDocument(Document document) {
        documentRepository.delete(document);
    }


}
