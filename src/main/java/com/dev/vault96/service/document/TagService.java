package com.dev.vault96.service.document;

import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.repository.document.TagRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public List<Tag> findTagsByOwner(String userId){
        return tagRepository.findTagsByOwner(userId);
    }

    public List<Tag> findTagsByOwnerAndNameLike(String userId, String name){
        return tagRepository.findTagsByOwnerAndNameLike(userId, name);
    }

    public void save(Tag tag) throws DuplicateKeyException{
        try{
            tagRepository.save(tag);
        }
        catch(DuplicateKeyException e) {
            throw e;
        }
    }
}
