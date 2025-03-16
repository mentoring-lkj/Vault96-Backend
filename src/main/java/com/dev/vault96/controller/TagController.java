package com.dev.vault96.controller;

import com.dev.vault96.controller.message.tag.AddTagRequest;
import com.dev.vault96.controller.message.tag.DeleteTagRequestBody;
import com.dev.vault96.controller.message.tag.UpdateTagRequest;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.document.TagService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final AuthService authService;
    private final DocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);

    @GetMapping()
    public ResponseEntity<List<Tag>> getTags(HttpServletRequest request ){
        String email = authService.extractEmailFromToken(request);
        if(email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        List<Tag> tags = tagService.findTagsByOwner(email);
        return ResponseEntity.ok(tags);
    }

    @PostMapping()
    public ResponseEntity<Void> addTag(HttpServletRequest request, @RequestBody AddTagRequest requestBody) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        logger.debug("tags/add/email : " + email);
        Tag exists = tagService.findTagByOwnerAndName(email, requestBody.getName());
        if (exists!=null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict 응답
        }

        Tag tag = new Tag();
        tag.setOwner(email);
        tag.setName(requestBody.getName());
        logger.debug("tag : " + tag.getName());

        try {
            tagService.save(tag);
            logger.debug("tagsaved : " + tag.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTag(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestBody UpdateTagRequest requestBody) {

        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean isUpdated = tagService.updateTag(email, id, requestBody.getName());
        return isUpdated ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }

    @PostMapping("/delete")
    @Transactional
    public ResponseEntity<Void> deleteTag(HttpServletRequest request, @RequestBody DeleteTagRequestBody requestBody) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        logger.debug("delete user email : " + email);
        logger.debug(requestBody.getTagIds().toString());
        List<Tag> tags = tagService.findTagsByOwnerAndTagIds(email, requestBody.getTagIds());
        logger.debug("tags length  : " + Integer.toString(tags.size()));
        tags.forEach(tagService::delete);

        List<Document> documents = documentService.findDocumentsByOwner(email);
        documents.forEach(document -> {
            document.getTags().removeIf(Objects::isNull);
            documentService.save(document);
        });
        return ResponseEntity.ok().build();
    }


}
