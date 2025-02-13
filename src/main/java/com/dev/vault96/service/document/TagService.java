package com.dev.vault96.service.document;

import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.repository.document.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public List<Tag> findTagsByUserId(String userId){
        return tagRepository.findTagsByUserId(userId);
    }

    public List<Tag> findTagsByUserIdAndNameLike(String userId, String name){
        return tagRepository.findTagsByUserIdAndNameLike(userId, name);
    }

    public Tag findTagByTagId(String tagId){
        Optional<Tag> tag = tagRepository.findTagByTagId(tagId);
        if(tag.isPresent()){return tag.get();}
        else{return null;}
    }
}
