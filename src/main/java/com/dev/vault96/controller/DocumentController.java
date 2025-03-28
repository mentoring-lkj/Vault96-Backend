package com.dev.vault96.controller;

import com.dev.vault96.controller.message.document.*;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.document.TagService;
import com.dev.vault96.service.s3.S3Service;
import com.dev.vault96.service.shared.SharedDocumentFolderService;
import com.dev.vault96.util.DocumentUtil;
import com.mongodb.DuplicateKeyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final SharedDocumentFolderService sharedDocumentFolderService;
    private final TagService tagService;
    private final DocumentUtil documentUtil;
    private final AuthService authService;
    private final S3Service s3Service;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(HttpServletRequest request, @PathVariable String id){
        String email = authService.extractEmailFromToken(request);
        Document document = documentService.findDocumentById(id);
        if(!document.getOwner().equals(email)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(document);
    }

    @GetMapping("/multiple")
    public ResponseEntity<List<Document>> getDocumentsByIds(HttpServletRequest request, @RequestBody List<String> ids){
        String email = authService.extractEmailFromToken(request);
        if(email == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        List<Document> documents = documentService.findAllByIdIn(ids);
        boolean isValid = documents.stream().allMatch(document -> document.getOwner().equals(email)) && documents.size() == ids.size();
        if(!isValid){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/search")
    public ResponseEntity<DocumentSearchResponseBody> searchDocumentsPage(
            HttpServletRequest request,
            @RequestBody DocumentSearchRequestBody requestBody) {

        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String name = requestBody.getName().orElse("").trim();
        List<String> tagIds = requestBody.getTagIds().orElse(Collections.emptyList());
        String nameNFD = Normalizer.normalize(name, Normalizer.Form.NFD);


        int page = requestBody.getPage();
        int size = requestBody.getSize();

        Page<Document> documentPage;
        long totalCount;

        if (name.isEmpty() && tagIds.isEmpty()) {
            documentPage = documentService.findAllDocuments(email, page, size);
            totalCount = documentService.countAllDocuments(email);
        } else if (tagIds.isEmpty()) {
            documentPage = documentService.findDocumentPageByOwnerAndNameLike(email, name, page, size);
            totalCount = documentService.countDocumentsByOwnerAndName(email, name);
        }
        else {
            documentPage = documentService.searchDocuments(email, name, tagIds, page, size);
            totalCount = documentService.countDocumentsByOwnerAndNameAndTags(email, name, tagIds);
        }


        DocumentSearchResponseBody responseBody = new DocumentSearchResponseBody();
        responseBody.setFiles(documentPage.getContent());
        responseBody.setHasNext(documentPage.hasNext());
        responseBody.setTotalCount((int) totalCount);

        return ResponseEntity.ok(responseBody);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestBody UpdateDocumentRequestBody requestBody) {

        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Document document = documentService.findDocumentById(id);
        if (!document.getOwner().equals(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        requestBody.getName().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                document.setName(name);
            }
        });

        List<String> tagIds = requestBody.getTagIds().orElse(Collections.emptyList());

        List<Tag> tags = tagService.findTagsByOwnerAndTagIds(email, tagIds); // Tag ID로 실제 엔티티 조회
        document.setTags(tags);

        try {
            documentService.save(document);
        } catch (Exception e) {
            logger.error("Error updating document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        return ResponseEntity.ok(document);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteDocument(HttpServletRequest request, @PathVariable String id) {
        try {
            String email = authService.extractEmailFromToken(request);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            boolean isDeleted = documentService.deleteDocument(email, id);
            return ResponseEntity.ok(isDeleted);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }  catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
    }
    @PostMapping("/upload/request")
    public ResponseEntity<CreateDocumentResponseBody> requestPresignedUrl(HttpServletRequest request,
                                                                          @RequestBody CreateDocumentRequestBody requestBody) {
        String email = authService.extractEmailFromToken(request);
        String fileName = requestBody.getName();

        if (documentService.findDocumentByOwnerAndName(email, fileName) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String presignedUrl = s3Service.getPresignedUploadUrl(email, fileName).toString();
        CreateDocumentResponseBody responseBody = new CreateDocumentResponseBody();
        responseBody.setPresignedUploadUrl(presignedUrl);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/upload/complete")
    public ResponseEntity<String> uploadAndMove(HttpServletRequest request, @RequestBody CreateDocumentRequestBody requestBody) {
        String email = authService.extractEmailFromToken(request);
        String fileName = requestBody.getName();
        String fileNameNFC = Normalizer.normalize(fileName, Normalizer.Form.NFC);


        try {
            documentService.createDocument(email, fileName, fileNameNFC);
            return ResponseEntity.ok("File uploaded and moved successfully");

        }catch(DuplicateKeyException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unavaiable file name: " );

        }
        catch (Exception e) {
            logger.error("File move failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File move failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<DownloadDocumentResponseBody> download(HttpServletRequest request, @PathVariable String id){
        String email = authService.extractEmailFromToken(request);
        if(email == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Document document = documentService.findDocumentById(id);
        if (document == null || !document.getOwner().equals(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String presignedDownloadUrl = s3Service.getDocumentPresignedDownloadUrl(email, document.getName(), document.getId()).toString();
        DownloadDocumentResponseBody responseBody = new DownloadDocumentResponseBody();
        responseBody.setPresignedDownloadUrl(presignedDownloadUrl);

        return ResponseEntity.ok(responseBody);

    }
}
