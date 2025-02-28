package com.dev.vault96.controller;

import com.dev.vault96.controller.message.document.*;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.document.Tag;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.document.TagService;
import com.dev.vault96.service.s3.S3Service;
import com.dev.vault96.util.DocumentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final TagService tagService;
    private final DocumentUtil documentUtil;
    private final AuthService authService;
    private final S3Service s3Service;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @PostMapping("/search")
    public ResponseEntity<DocumentSearchResponseBody> searchDocumentsPage(
            HttpServletRequest request,
            @RequestBody DocumentSearchRequestBody requestBody) {

        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Optional을 활용한 값 처리
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

    @GetMapping("/shared")
    public ResponseEntity<List<Document>> getSharedDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsBySharedMember(email);
        return ResponseEntity.ok(documents);
    }
    @PutMapping("/update/{id}")
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

        // ✅ `Optional`을 활용하여 값이 존재할 때만 업데이트
        requestBody.getName().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                document.setName(name);
            }
        });

        // ✅ 태그 ID 리스트를 실제 `Tag` 객체 리스트로 변환하여 저장
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
    @PostMapping("/delete/{id}")
    public ResponseEntity<Boolean> deleteDocument(HttpServletRequest request,
                                               @PathVariable String id
                                               ) {
        try {
            String email = authService.extractEmailFromToken(request);
            if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            Document document = documentService.findDocumentById(id);
            if(!document.getOwner().equals(email)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);

            }
            documentService.deleteDocument(document);
            s3Service.deleteFile(email, document.getId());
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
        return ResponseEntity.ok(true);
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
        logger.debug("complete file : " + requestBody.getName());
        String email = authService.extractEmailFromToken(request);
        String fileName = requestBody.getName();

        // 파일이 존재하는지 확인
        if (!s3Service.doesFileExist(email, fileName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File not uploaded");
        }

        String fileNameNFC = Normalizer.normalize(fileName, Normalizer.Form.NFC);


        Document document = new Document();
        document.setName(fileNameNFC);
        document.setOwner(email);
        document.setFormat(documentUtil.getFileExtension(fileName));
        document.setCreatedAt(new Date());
        document.setTags(new ArrayList<>());
        document.setSharedMembers(new ArrayList<>());

        documentService.save(document);

        document.setSize(s3Service.getFileSize(email, document.getId()));
        logger.debug("Document Key : " + document.getId());
        s3Service.moveFileToDocuments(email, fileName, document.getId());

        return ResponseEntity.ok("File uploaded and moved successfully");    }

    @PostMapping("/download")
    public ResponseEntity<DownloadDocumentResponseBody> download(HttpServletRequest request, @RequestBody DownloadDocumentRequestBody requestBody){
        String email = authService.extractEmailFromToken(request);
        if (documentService.findDocumentByOwnerAndName(email, requestBody.getName()) == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        Document document = documentService.findDocumentByOwnerAndName(email, requestBody.getName());

        String presignedDownloadUrl = s3Service.getPresignedDownloadUrl(email,document.getName(), document.getId()).toString();
        DownloadDocumentResponseBody responseBody = new DownloadDocumentResponseBody();
        responseBody.setPresignedDownloadUrl(presignedDownloadUrl);

        return ResponseEntity.ok(responseBody);

    }
}
