package com.dev.vault96.service.member;

import com.dev.vault96.dto.document.Document;
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
        Optional<List<Document>> documents = documentRepository.findDocumentsByOwner(owner);
        if(documents.isPresent()) return documents.get();
        else return null;
    }

    public List<Document> findDocumentByOwnerAndNameLike(String owner, String name){
        Optional<List<Document>> documents = documentRepository.findDocumentByOwnerAndNameLike(owner, name);
        if(documents.isPresent()) return documents.get();
        else return null;
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

    public void save(Document document){
        documentRepository.save(document);
    }


}
