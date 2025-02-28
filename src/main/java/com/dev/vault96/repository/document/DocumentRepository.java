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

    // 🔹 특정 사용자의 전체 문서 개수 조회
    long countByOwner(String email);

    // 🔹 특정 이름을 포함하는 문서 개수 조회
    long countByOwnerAndNameContaining(String email, String name);

    // 🔹 특정 태그를 포함하는 문서 개수 조회
    @Query(value = "{ 'owner': ?0, 'tags': { $in: ?1 } }", count = true)
    long countByOwnerAndTags(String email, List<String> tagIds);

    // 🔹 특정 이름과 태그를 동시에 만족하는 문서 개수 조회
    @Query(value = "{ 'owner': ?0, 'name': { $regex: ?1, $options: 'i' }, 'tags': { $in: ?2 } }", count = true)
    long countByOwnerAndNameAndTags(String email, String name, List<String> tagIds);

    // 🔹 문서 ID로 조회
    Optional<Document> findDocumentById(String id);

    // 🔹 특정 사용자의 특정 이름을 가진 문서 조회 (정확한 일치)
    @Query("{'owner': ?0, 'name': ?1}")
    Optional<Document> findDocumentByOwnerAndName(String owner, String name);

    // 🔹 특정 사용자의 모든 문서 조회 (페이징 X)
    List<Document> findDocumentsByOwner(String owner);

    // 🔹 특정 이름을 포함하는 문서 검색 (대소문자 구분 X, 페이징 X)
    @Query("{'owner': ?0, 'name': {$regex: ?1, $options: 'i'}}")
    List<Document> findDocumentsByOwnerAndNameRegex(String owner, String name);

    // 🔹 특정 태그를 포함하는 문서 검색
    @Query("{'owner': ?0, 'tags': { $in: ?1 }}")
    List<Document> findDocumentsByOwnerAndTagsContaining(String owner, List<String> tagIds);

    // 🔹 특정 태그 목록을 **모두 포함하는** 문서 검색
    @Query("{'owner': ?0, 'tags': { $all: ?1 }}")
    List<Document> findDocumentsByOwnerAndTagsContainingAll(String owner, List<String> tagIds);

    // 🔹 특정 사용자가 공유된 문서 조회
    List<Document> findDocumentsBySharedMembersContaining(String email);

    // 🔹 특정 문서를 소유자와 이름으로 삭제
    void deleteDocumentByOwnerAndName(String email, String name);
}
