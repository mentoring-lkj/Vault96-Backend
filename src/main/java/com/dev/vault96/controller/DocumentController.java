package com.dev.vault96.controller;

import com.dev.vault96.controller.message.CreateDocumentRequestBody;
import com.dev.vault96.controller.message.CreateDocumentResponseBody;
import com.dev.vault96.controller.message.DownloadDocumentResponseBody;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.s3.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final AuthService authService;
    private final S3Service s3Service;

    /**
     * ✅ 1. 사용자의 모든 문서 조회 (GET /api/documents)
     */
    @GetMapping
    public ResponseEntity<List<Document>> getUserDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsByOwner(email);
        return ResponseEntity.ok(documents);
    }

    /**
     * ✅ 2. 문서 이름으로 검색 (GET /api/documents/search?name=filename)
     */
    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocumentsByName(HttpServletRequest request,
                                                                @RequestParam String name) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentByOwnerAndNameLike(email, name);
        return ResponseEntity.ok(documents);
    }

    /**
     * ✅ 3. 특정 태그가 포함된 문서 검색 (GET /api/documents/search/tags?tagIds=tag1,tag2)
     */
    @GetMapping("/search/tags")
    public ResponseEntity<List<Document>> searchDocumentsByTags(HttpServletRequest request,
                                                                @RequestParam List<String> tagIds) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsContatinTags(email, tagIds);
        return ResponseEntity.ok(documents);
    }

    /**
     * ✅ 4. 공유된 문서 조회 (GET /api/documents/shared)
     */
    @GetMapping("/shared")
    public ResponseEntity<List<Document>> getSharedDocuments(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Document> documents = documentService.findDocumentsBySharedMember(email);
        return ResponseEntity.ok(documents);
    }

    /**
     * ✅ 5. 문서 이름 변경 (PUT /api/documents/updateName)
     */
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

    /**
     * ✅ 6. 문서 삭제 (DELETE /api/documents/delete)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteDocument(HttpServletRequest request,
                                               @RequestParam String name) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Document document = documentService.findDocumentByOwnerAndName(email, name);
        documentService.deleteDocument(document);
        return ResponseEntity.ok(null);
    }

    /**
     * ✅ 7. 문서에 태그 추가 (POST /api/documents/addTag)
     */
    @PostMapping("/addTag")
    public ResponseEntity<Void> addTagToDocument(HttpServletRequest request,
                                                 @RequestParam String name,
                                                 @RequestParam String tagId) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Document document = documentService.findDocumentByOwnerAndName(email, name);
        boolean updated = documentService.addTagToDocument(document.getId(), tagId);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }

    /**
     * ✅ 8. Presigned URL 생성 (POST /api/documents/upload/request)
     */
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

    /**
     * ✅ 9. 파일 업로드 완료 후 이동 처리 (POST /api/documents/upload/complete)
     */
    @PostMapping("/upload/complete")
    public ResponseEntity<String> uploadAndMove(HttpServletRequest request, @RequestBody CreateDocumentRequestBody requestBody) {
        String email = authService.extractEmailFromToken(request);
        String name = requestBody.getName();
        s3Service.moveFileToDocuments(email, name);
        Document document = new Document();
        document.setName(name);
        document.setOwner(email);
        document.setFormat(documentService.getFileExtension(name));
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
