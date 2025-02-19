package com.dev.vault96.repository.document;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    Optional<Document> findDocumentById(String id);

    @Query("{'owner': ?0, 'name': ?1}")
    Optional<Document> findDocumentByOwnerAndName(String owner, String name);

    List<Document> findDocumentsByOwner(String owner);

    @Query("{'owner': ?0, 'name': {$regex: ?1, $options: 'i'}}")
    List<Document> findDocumentsByOwnerAndNameLike(String owner, String name);

    @Query("{'owner': ?0, 'tags': {$in: [?1]}}")
    List<Document> findDocumentsByOwnerAndTagsContaining(String owner, Tag tag);

    @Query("{'owner': ?0, 'tags': {$all: ?1}}")
    List<Document> findDocumentsByOwnerAndTagsContainingAll(String owner, List<String> tagIds);

    List<Document> findDocumentsBySharedMembersContaining(String email);


    //List<Document> findDocumentsByOwnerAndTagsContainsAndNameLike()

    void deleteDocumentByOwnerAndName(String email, String name);

}
