package com.dev.vault96.controller;

import com.dev.vault96.controller.message.document.CreateDocumentRequestBody;
import com.dev.vault96.controller.message.document.CreateDocumentResponseBody;
import com.dev.vault96.controller.message.document.DocumentSearchRequestBody;
import com.dev.vault96.controller.message.document.DownloadDocumentResponseBody;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.s3.S3Service;
import com.dev.vault96.util.DocumentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final DocumentUtil documentUtil;
    private final AuthService authService;
    private final S3Service s3Service;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @GetMapping
    public ResponseEntity<List<Document>> getUserDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsByOwner(email);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            HttpServletRequest request,
            @RequestBody DocumentSearchRequestBody requestBody) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean isNameEmpty = requestBody.getName() == null || requestBody.getName().trim().isEmpty();
        boolean isTagsEmpty = requestBody.getTags() == null || requestBody.getTags().isEmpty();

        //  검색 조건이 둘 다 비어 있으면 모든 문서 반환
        if (isNameEmpty && isTagsEmpty) {
            List<Document> allDocuments = documentService.findDocumentsByOwner(email);
            logger.debug("Returning all documents: " + allDocuments.size());
            return ResponseEntity.ok(allDocuments);
        }

        //  개별 조건으로 검색 수행
        List<Document> documentsByName = isNameEmpty ? new ArrayList<>() : documentService.findDocumentByOwnerAndNameLike(email, requestBody.getName());
        List<Document> documentsByTags = isTagsEmpty ? new ArrayList<>() : documentService.findDocumentsContatinTags(email, requestBody.getTags());

        //  교집합 구하기
        Set<Document> resultDocuments;
        if (!isNameEmpty && !isTagsEmpty) {
            resultDocuments = new HashSet<>(documentsByName);
            resultDocuments.retainAll(documentsByTags); // 🔥 교집합 유지
        } else {
            resultDocuments = new HashSet<>();
            resultDocuments.addAll(documentsByName);
            resultDocuments.addAll(documentsByTags);
        }

        logger.debug("Final result size: " + resultDocuments.size());
        return ResponseEntity.ok(new ArrayList<>(resultDocuments));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<Document>> getSharedDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsBySharedMember(email);
        return ResponseEntity.ok(documents);
    }

    @PutMapping("/updateName")
    public ResponseEntity<Void> updateDocumentName(HttpServletRequest request,
                                                   @RequestParam String name,
                                                   @RequestParam String newName) {
        String email = authService.extractEmailFromToken(request);
        Document document = documentService.findDocumentByOwnerAndName(email, name);
        Document newNameDocument = documentService.findDocumentByOwnerAndName(email, newName);
        if(newNameDocument != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((null));
        }
        documentService.updateDocumentName(document.getId(), newName);
        s3Service.moveFile(email, name, newName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteDocument(HttpServletRequest request,
                                               @RequestParam String name) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Document document = documentService.findDocumentByOwnerAndName(email, name);
        documentService.deleteDocument(document);
        return ResponseEntity.ok(null);
    }


    @PostMapping("/addTag")
    public ResponseEntity<Void> addTagToDocument(HttpServletRequest request,
                                                 @RequestParam String name,
                                                 @RequestParam String tagName) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Document document = documentService.findDocumentByOwnerAndName(email, name);
        boolean updated = documentService.addTagToDocument(document.getId(), tagName);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
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
        String name = requestBody.getName();
        s3Service.moveFileToDocuments(email, name);
        Document document = new Document();
        document.setName(name);
        document.setOwner(email);
        document.setFormat(documentUtil.getFileExtension(name));
        document.setCreatedAt(new Date());
        document.setTags(requestBody.getTags());
        document.setSize(s3Service.getFileSize("vault96-bucket", email, name));
        document.setSharedMembers(new ArrayList<>());
        documentService.save(document);
        return ResponseEntity.ok("File uploaded and moved successfully");
    }

    @GetMapping("/download")
    public ResponseEntity<DownloadDocumentResponseBody> download(HttpServletRequest request, @RequestParam String name){
        String email = authService.extractEmailFromToken(request);
        if (documentService.findDocumentByOwnerAndName(email, name) == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String presignedDownloadUrl = s3Service.getPresignedDownloadUrl(email, name).toString();
        DownloadDocumentResponseBody responseBody = new DownloadDocumentResponseBody();
        responseBody.setPresignedDownloadUrl(presignedDownloadUrl);

        return ResponseEntity.ok(responseBody);

    }
}
