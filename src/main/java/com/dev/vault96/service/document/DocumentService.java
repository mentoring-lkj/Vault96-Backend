package com.dev.vault96.service.document;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.repository.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final MongoTemplate mongoTemplate;

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

    public void updateDocumentName(String documentId, String newName){
        Document document = documentRepository.findDocumentById(documentId).get();
        document.setName(newName);
        documentRepository.save(document);
    }

    public boolean addTagToDocument(String documentId, String tagId) {
        Query query = new Query(Criteria.where("id").is(documentId));
        Update update = new Update().addToSet("tags", tagId); // 중복 없이 추가
        return mongoTemplate.updateFirst(query, update, Document.class).getModifiedCount() > 0;
    }

    public List<Document> findDocumentsContainTag(String owner, String tagId){
        List<Document> documents = documentRepository.findDocumentsByOwnerAndTagsContaining(owner, tagId);
        return documents;
    }

    public List<Document> findDocumentsContatinTags(String owner, List<String> tagIds){
        List<Document> documents = documentRepository.findDocumentsByOwnerAndTagsContainingAll(owner, tagIds);
        return documents;
    }

    public List<Document> findDocumentsBySharedMember(String sharedMember){
        List<Document> documents = documentRepository.findDocumentsBySharedMembersContaining(sharedMember);
        return documents;
    }

    public void save(Document document){
        documentRepository.save(document);
    }

}
