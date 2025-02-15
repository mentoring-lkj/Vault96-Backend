package com.dev.vault96.controller;

import com.dev.vault96.controller.message.tag.AddTagRequest;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.TagService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);

    @GetMapping()
    public ResponseEntity<List<Tag>> getTags(HttpServletRequest request ){
        String email = authService.extractEmailFromToken(request);
        if(email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        List<Tag> tags = tagService.findTagsByOwner(email);
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addTag(HttpServletRequest request, @RequestBody AddTagRequest requestBody){
        String email = authService.extractEmailFromToken(request);
        if(email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        Tag tag = new Tag();
        tag.setOwner(email);
        tag.setName(requestBody.getName());
        try{
            tagService.save(tag);
            return ResponseEntity.ok(null);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


}
