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
    List<Tag> findAllByIdIn(List<String> tagIds);
    Optional<Tag> findTagByOwnerAndName(String email, String name);
    @Query("{'owner' : ?0, 'name' : {$regex: ?1, $options: 'i'}}")
    List<Tag> findTagsByOwnerAndNameLike(String email, String name);

    @Query("{ $and: [ { 'owner': ?0 }, { '_id': { $in: ?1 } } ] }")
    List<Tag> findTagsByOwnerAndIds(String owner, List<String> tagIds);


    Optional<Tag> findTagByOwnerAndId(String owner, String id);


}
