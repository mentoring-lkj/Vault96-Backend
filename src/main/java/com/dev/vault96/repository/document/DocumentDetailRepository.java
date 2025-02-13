package com.dev.vault96.repository.document;

import com.dev.vault96.entity.document.DocumentDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentDetailRepository extends MongoRepository<DocumentDetail, String> {

    Optional<DocumentDetail> findDocumentDetailById(String id);
    List<DocumentDetail> findDocumentDetailsByDocumentId(String id);
    Optional<DocumentDetail> findDocumentDetailByDocumentIdAndVersion(String documentId, String version);

}
