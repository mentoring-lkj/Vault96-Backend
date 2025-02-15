package com.dev.vault96.repository.document;

import com.dev.vault96.entity.document.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    List<Tag> findTagsByOwner(String userId);

    @Query("{'owner' : ?0, 'name' : {$regex: ?1, $options: 'i'}}")
    List<Tag> findTagsByOwnerAndNameLike(String userId, String name);

}
