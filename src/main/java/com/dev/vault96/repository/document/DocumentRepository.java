package com.dev.vault96.repository.document;

import com.dev.vault96.entity.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    // 🔹 특정 사용자의 문서 목록 조회 (페이징)
    @Query("{'owner': ?0}")
    Page<Document> findAllByOwner(String owner, Pageable pageable);

    // 🔹 특정 이름과 태그를 기반으로 문서 검색 (페이징)
    @Query("{'owner': ?0, 'name': {$regex: ?1, $options: 'i'}, 'tags': {$all: ?2}}")
    Page<Document> searchDocuments(String owner, String name, List<String> tagIds, Pageable pageable);

    @Query("{'owner': ?0, 'name': {$regex: ?1, $options: 'i'}}")
    Page<Document> findDocumentsByOwnerAndNameLike(String owner, String name, Pageable pageable);

    List<Document> findAllByIdIn(List<String> ids);

    long countByOwner(String email);

    long countByOwnerAndNameContaining(String email, String name);

    @Query(value = "{ 'owner': ?0, 'tags': { $in: ?1 } }", count = true)
    long countByOwnerAndTags(String email, List<String> tagIds);

    @Query(value = "{ 'owner': ?0, 'name': { $regex: ?1, $options: 'i' }, 'tags': { $in: ?2 } }", count = true)
    long countByOwnerAndNameAndTags(String email, String name, List<String> tagIds);

    Optional<Document> findDocumentById(String id);

    @Query("{'owner': ?0, 'name': ?1}")
    Optional<Document> findDocumentByOwnerAndName(String owner, String name);

    List<Document> findDocumentsByOwner(String owner);

    @Query("{'owner': ?0, 'name': {$regex: ?1, $options: 'i'}}")
    List<Document> findDocumentsByOwnerAndNameRegex(String owner, String name);

    @Query("{'owner': ?0, 'tags': { $in: ?1 }}")
    List<Document> findDocumentsByOwnerAndTagsContaining(String owner, List<String> tagIds);

    @Query("{'owner': ?0, 'tags': { $all: ?1 }}")
    List<Document> findDocumentsByOwnerAndTagsContainingAll(String owner, List<String> tagIds);

    List<Document> findDocumentsBySharedMembersContaining(String email);

    void deleteDocumentByOwnerAndName(String email, String name);
}
