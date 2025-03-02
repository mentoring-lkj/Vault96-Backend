package com.dev.vault96.controller;

import com.dev.vault96.controller.message.shared.*;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.s3.S3Service;
import com.dev.vault96.service.shared.SharedDocumentFolderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/shared")
@RequiredArgsConstructor
public class SharedController {
    private final SharedDocumentFolderService sharedDocumentFolderService;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(SharedController.class);


    @GetMapping
    public ResponseEntity<GetSharedDocumentFoldersResponse> getSharedDocumentFolders(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        List<SharedDocumentFolder> sharedFolders = sharedDocumentFolderService.findSharedDocumentFoldersByOwner(email);
        GetSharedDocumentFoldersResponse response = new GetSharedDocumentFoldersResponse();
        response.setSharedDocumentFolders(sharedFolders);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetSharedDocumentFolderResponse> getSharedDocumentFolder(HttpServletRequest request, @PathVariable String id){
        String email = authService.extractEmailFromToken(request);
        SharedDocumentFolder sharedDocumentFolder = sharedDocumentFolderService.findSharedDocumentFolderById(id);
        if(sharedDocumentFolder == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        if(!sharedDocumentFolder.getOwner().equals(email)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        GetSharedDocumentFolderResponse response = new GetSharedDocumentFolderResponse();
        response.setSharedDocumentFolder(sharedDocumentFolder);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}/download")
    public ResponseEntity<GetSharedDocumentFolderDownloadURLResponse> getSharedDocumentFolderDownloadLink(HttpServletRequest request, @PathVariable String id){
        String email = authService.extractEmailFromToken(request);
        SharedDocumentFolder sharedDocumentFolder = sharedDocumentFolderService.findSharedDocumentFolderByIdAndOwner(id, email);
        if(sharedDocumentFolder == null){
            logger.debug("no such sharedFolder");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        else{
            String presignedURL = sharedDocumentFolderService.getPresignedDownloadURL(email, sharedDocumentFolder.getName(), sharedDocumentFolder.getId());
            logger.debug(presignedURL);
            GetSharedDocumentFolderDownloadURLResponse response = new GetSharedDocumentFolderDownloadURLResponse();
            response.setPresignedURL(presignedURL);
            logger.debug("responding");
            return ResponseEntity.ok(response);

        }


    }

    @PostMapping
    public ResponseEntity<CreateSharedDocumentFolderResponse> createSharedDocumentFolders(HttpServletRequest request, @RequestBody CreateSharedDocumentFolderRequest requestBody) {
        String email = authService.extractEmailFromToken(request);
        CreateSharedDocumentFolderResponse response = new CreateSharedDocumentFolderResponse();

        try{
            SharedDocumentFolder folder = sharedDocumentFolderService.createSharedFolder(email, requestBody.getName()+".zip", requestBody.getDocuments());

        }catch(IOException e){
            response.setResult(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        response.setResult(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Boolean> deleteSharedDocumentFolders(HttpServletRequest request, @PathVariable String id){
        String email = authService.extractEmailFromToken(request);
        boolean result = sharedDocumentFolderService.deleteSharedDocument(email, id);
        if(result == true){
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
    }
}
