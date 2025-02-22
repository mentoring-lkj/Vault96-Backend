package com.dev.vault96.controller;

import com.dev.vault96.controller.message.document.*;
import com.dev.vault96.controller.message.tag.AddTagRequest;
import com.dev.vault96.controller.message.tag.UpdateTagRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<List<Document>> getUserDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsByOwner(email);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/search")
    public ResponseEntity<DocumentSearchResponseBody> searchDocuments(
            HttpServletRequest request,
            @RequestBody DocumentSearchRequestBody requestBody) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        DocumentSearchResponseBody responseBody = new DocumentSearchResponseBody();

        boolean isNameEmpty = requestBody.getName() == null || requestBody.getName().trim().isEmpty();
        boolean isTagsEmpty = requestBody.getTags() == null || requestBody.getTags().isEmpty();

        if (isNameEmpty && isTagsEmpty) {
            List<Document> allDocuments = documentService.findDocumentsByOwner(email);
            logger.debug("Returning all documents: " + allDocuments.size());
            responseBody.setFiles(allDocuments);
            responseBody.setTotalCount(allDocuments.size());
            responseBody.setHasNext(false);
            return ResponseEntity.ok(responseBody);
        }

        // 🔹 개별 검색 수행
        List<Document> documentsByName = isNameEmpty
                ? new ArrayList<>()
                : documentService.findDocumentByOwnerAndNameLike(email, requestBody.getName());

        List<Document> documentsByTags = isTagsEmpty
                ? new ArrayList<>()
                : documentService.findDocumentsContatinTags(email, requestBody.getTags());

        Set<Document> resultDocuments;
        if (!isNameEmpty && !isTagsEmpty) {
            resultDocuments = new HashSet<>(documentsByName);
            resultDocuments.retainAll(documentsByTags);
        } else {
            resultDocuments = new HashSet<>();
            resultDocuments.addAll(documentsByName);
            resultDocuments.addAll(documentsByTags);
        }
        List<Document> documents =  new ArrayList<>(resultDocuments);
        responseBody.setFiles(documents);
        responseBody.setTotalCount(documents.size());
        responseBody.setHasNext(false);
        logger.debug("Final result size: " + resultDocuments.size());
        return ResponseEntity.ok(responseBody);
    }


    @GetMapping("/shared")
    public ResponseEntity<List<Document>> getSharedDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsBySharedMember(email);
        return ResponseEntity.ok(documents);
    }
    @PostMapping("/updateDocument")
    public ResponseEntity<Document> updateDocument(HttpServletRequest request, @RequestBody EditDocumentRequestBody requestBody){
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Document document = documentService.findDocumentByOwnerAndName(email, requestBody.getName());

        if(requestBody.getNewName()!= null && !requestBody.getNewName().equals("")){
            document.setName(requestBody.getNewName());
        }
        document.setTags(requestBody.getTags());
        try{
            documentService.save(document);
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        return ResponseEntity.ok(document);
    }

    @PostMapping("/delete")
    public ResponseEntity<Boolean> deleteDocument(HttpServletRequest request,
                                               @RequestBody DeleteDocumentRequestBody requestBody) {
        try {
            String email = authService.extractEmailFromToken(request);
            if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            Document document = documentService.findDocumentByOwnerAndName(email, requestBody.getName());
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

        Document document = new Document();
        document.setName(fileName);
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
