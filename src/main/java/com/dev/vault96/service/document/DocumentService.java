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


    public Page<Document> findAllDocuments(String owner, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findAllByOwner(owner, pageable);
    }

    public Page<Document> searchDocuments(String owner, String name, List<String> tagIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.searchDocuments(owner, name, tagIds, pageable);
    }

    public long countDocuments(String owner, String name, List<String> tagIds) {
        return documentRepository.countDocuments(owner, name, tagIds);
    }

    public Document findByOwnerAndName(String owner, String name){
        Optional<Document> document =  documentRepository.findDocumentByOwnerAndName(owner, name);
        if(document.isPresent()) return document.get();
        else return null;
    }

    public List<Document> findDocumentsByOwner(String owner){
        List<Document> documents = documentRepository.findDocumentsByOwner(owner);
        return documents;
    }

    public List<Document> findDocumentByOwnerAndNameLike(String owner, String name){
        List<Document> documents = documentRepository.findDocumentsByOwnerAndNameLike(owner, name);
        return documents;
    }

    public Document findDocumentByOwnerAndName(String email, String name){
        Optional<Document> document = documentRepository.findDocumentByOwnerAndName(email, name);
        if(document.isPresent()){
            return document.get();
        }
        else return null;
    }

    public void updateDocumentName(String documentId, String newName){
        Document document = documentRepository.findDocumentById(documentId).get();
        document.setName(newName);
        documentRepository.save(document);
    }

    public boolean addTagToDocument(String documentId, Tag tag) {
        Query query = new Query(Criteria.where("id").is(documentId));
        Update update = new Update().addToSet("tags", tag); // 중복 없이 추가
        return mongoTemplate.updateFirst(query, update, Document.class).getModifiedCount() > 0;
    }

    public List<Document> findDocumentsContainTag(String owner, Tag tag){
        List<Document> documents = documentRepository.findDocumentsByOwnerAndTagsContaining(owner, tag);
        return documents;
    }

    public List<Document> findDocumentsContatinTags(String owner, List<Tag> tags) {
        // 🔹 태그 ID 리스트로 변환
        List<String> tagIds = tags.stream().map(Tag::getId).toList();

        // 🔹 `tagIds`를 기반으로 검색
        return documentRepository.findDocumentsByOwnerAndTagsContainingAll(owner, tagIds);
    }


    public List<Document> findDocumentsBySharedMember(String sharedMember){
        List<Document> documents = documentRepository.findDocumentsBySharedMembersContaining(sharedMember);
        return documents;
    }

    public void save(Document document) throws DuplicateKeyException{

        try{
            documentRepository.save(document);
        }catch(DuplicateKeyException e){
            throw e;
        }
    }

    public void deleteDocument(Document document){
        documentRepository.delete(document);
    }

    public void removeNullTagsFromDocuments(String owner) {
        // `owner`에 해당하는 문서에서 `tags` 배열의 `null` 값을 제거하는 쿼리
        Query query = new Query(Criteria.where("owner").is(owner));

        // `tags` 배열에서 `null`을 제거
        Update update = new Update().pull("tags", null);
        mongoTemplate.updateMulti(query, update, Document.class);

        // `tags` 배열이 비었을 경우 빈 배열로 설정
        query = new Query(Criteria.where("tags").size(0)); // `tags` 배열이 빈 경우
        update = new Update().set("tags", new ArrayList<Tag>());
        mongoTemplate.updateMulti(query, update, Document.class);
    }

    public void removeTagsFromDocuments(String owner, List<String> tagIds) {
        // 사용자의 문서를 찾는 쿼리
        Query query = new Query(Criteria.where("owner").is(owner));

        // 해당 태그들을 `tags` 배열에서 제거하는 쿼리
        Update update = new Update().pull("tags", new Query(Criteria.where("id").in(tagIds)));
        mongoTemplate.updateMulti(query, update, Document.class);

        // null 값만 있는 tags 배열에서 null 값 제거
        query = new Query(Criteria.where("tags").is(null));
        update = new Update().pull("tags", null); // null 값을 삭제
        mongoTemplate.updateMulti(query, update, Document.class);

        // `tags` 배열이 비었을 경우 빈 배열로 설정
        query = new Query(Criteria.where("tags").size(0)); // `tags` 배열이 빈 경우
        update = new Update().set("tags", new ArrayList<Tag>());
        mongoTemplate.updateMulti(query, update, Document.class);
    }



}
