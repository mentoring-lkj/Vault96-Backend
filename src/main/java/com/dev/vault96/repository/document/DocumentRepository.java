package com.dev.vault96.repository.document;
import com.dev.vault96.dto.document.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    Optional<Document> findDocumentById(String id);
    // 1. owner + name으로 문서 검색 (정확한 일치)
    @Query("{'owner': ?0, 'name': ?1}")
    Optional<Document> findDocumentByOwnerAndName(String owner, String name);

    // 2. owner 기준으로 문서 검색 (부분 검색 가능)
    Optional<List<Document>> findDocumentsByOwner(String owner);

    // 3. owner + name의 일부만 포함하는 검색 (부분 검색)
    @Query("{'owner': ?0, 'name': {$regex: ?1, $options: 'i'}}")
    Optional<List<Document>> findDocumentByOwnerAndNameLike(String owner, String name);

}