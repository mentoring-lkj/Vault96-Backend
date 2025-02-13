package com.dev.vault96.controller;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.document.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("/api/document")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final AuthService authService;


    @GetMapping("/myDocuments")
    public ResponseEntity<List<Document>> getMemberDocuments(HttpServletRequest request){
        String email = authService.extractEmailFromToken(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Document> documents = documentService.findDocumentsByOwner(email);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/search/byName")
    public ResponseEntity<List<Document>> searchDocumentsByName(HttpServletRequest request,
                                                                @RequestParam String name) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Document> documents = documentService.findDocumentByOwnerAndNameLike(email, name);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/search/byTags")
    public ResponseEntity<List<Document>> searchDocumentsByTags(HttpServletRequest request,
                                                                @RequestParam List<String> tagIds){
        String email = authService.extractEmailFromToken(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Document> documents = documentService.findDocumentsContatinTags(email, tagIds);
        return ResponseEntity.ok(documents);

    }

    @GetMapping("/shared")
    public ResponseEntity<List<Document>> getSharedDocuments(HttpServletRequest request){
        String email = authService.extractEmailFromToken(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Document> documents = documentService.findDocumentsBySharedMember(email);
        return ResponseEntity.ok(documents);
    }

    // ✅ 문서 이름 업데이트 API
    @PutMapping("/updateName")
    public ResponseEntity<Void> updateDocumentName(HttpServletRequest request,
                                                   @RequestParam String documentId,
                                                   @RequestParam String newName) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        documentService.updateDocumentName(documentId, newName);
        return ResponseEntity.ok().build();
    }

    // ✅ 문서에 태그 추가 API
    @PostMapping("/addTag")
    public ResponseEntity<Void> addTagToDocument(HttpServletRequest request,
                                                 @RequestParam String documentId,
                                                 @RequestParam String tagId) {
        String email = authService.extractEmailFromToken(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean updated = documentService.addTagToDocument(documentId, tagId);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
}
