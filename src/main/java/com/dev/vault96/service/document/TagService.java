package com.dev.vault96.service.document;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.repository.document.DocumentRepository;
import com.dev.vault96.repository.document.TagRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final DocumentRepository documentRepository;
    private final MongoTemplate mongoTemplate;
    public List<Tag> findTagsByOwner(String userId){
        return tagRepository.findTagsByOwner(userId);
    }

    public Tag findTagByOwnerAndName(String email, String name){
        Optional<Tag> tag =  tagRepository.findTagByOwnerAndName(email, name);
        if(tag.isPresent()) return tag.get();
        return null;
    }

    public List<Tag> findTagsByOwnerAndNameLike(String userId, String name){
        return tagRepository.findTagsByOwnerAndNameLike(userId, name);
    }

    public boolean updateTag(String owner, String tagId, String newName) {
        Optional<Tag> optionalTag = tagRepository.findById(tagId);
        if (optionalTag.isEmpty()) {
            return false;
        }

        Tag tag = optionalTag.get();

        // 🔹 태그 소유자 확인
        if (!tag.getOwner().equals(owner)) {
            return false;
        }

        // 🔹 이름 업데이트 (MongoDB의 `Tag` 컬렉션 업데이트)
        tag.setName(newName);
        tagRepository.save(tag);

        // 🔹 `Document`의 `tags` 목록 업데이트 (태그가 포함된 모든 문서 찾기)
        List<Document> documentsWithTag = mongoTemplate.find(
                new Query(Criteria.where("tags").is(tag)), Document.class
        );

        for (Document doc : documentsWithTag) {
            // 기존 태그 리스트에서 해당 태그를 찾아 업데이트
            List<Tag> updatedTags = doc.getTags().stream()
                    .map(t -> t.getId().equals(tagId) ? tag : t) // ✅ 기존 태그 ID가 일치하면 업데이트된 태그 사용
                    .toList();

            doc.setTags(updatedTags);
            documentRepository.save(doc); // ✅ 변경된 문서 저장
        }

        return true;
    }


    public void save(Tag tag) throws DuplicateKeyException{
        try{
            tagRepository.save(tag);
        }
        catch(DuplicateKeyException e) {
            throw e;
        }
    }

    public void delete(Tag tag) {
        // 1. 태그 삭제
        tagRepository.delete(tag);

        // 2. 해당 태그를 참조하는 문서들의 tags 배열에서 해당 태그를 제거
// Update to remove any null values from the tags array
        Query query = new Query(Criteria.where("tags._id").is(tag.getId()));
        Update update = new Update().pull("tags", new Query(Criteria.where("_id").is(tag.getId())));
// Update the document without leaving null
        mongoTemplate.updateMulti(query, update, Document.class);

// Optionally, ensure that after deletion, the tags array is not null or empty
    }

}
