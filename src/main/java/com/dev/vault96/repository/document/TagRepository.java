package com.dev.vault96.repository.document;

import com.dev.vault96.dto.document.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    Optional<List<Tag>> findTagByUserId(String userId);
    Optional<Tag> findTagByUserIdAndName(String userId, String name);
    Optional<Tag> findTagByTagId(String tagId);

}
