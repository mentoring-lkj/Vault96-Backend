package com.dev.vault96.controller;

import com.dev.vault96.controller.message.member.MemberInfo;
import com.dev.vault96.controller.message.shared.*;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.entity.user.Member;
import com.dev.vault96.service.AuthService;
import com.dev.vault96.service.member.MemberService;
import com.dev.vault96.service.shared.SharedDocumentFolderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
    private final MemberService memberService;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(SharedController.class);


    @GetMapping
    public ResponseEntity<GetSharedDocumentFoldersResponse> getSharedDocumentFolders(HttpServletRequest request) {
        String email = authService.extractEmailFromToken(request);
        List<SharedDocumentFolder> sharedFolders = sharedDocumentFolderService.findSharedDocumentFoldersByOwner(email);
        GetSharedDocumentFoldersResponse response = new GetSharedDocumentFoldersResponse();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/publicShared/{id}")
    public ResponseEntity<GetSharedDocumentFolderResponse> getPublicSharedFolder(HttpServletRequest request, @PathVariable String id){
        SharedDocumentFolder sharedDocumentFolder = sharedDocumentFolderService.findSharedDocumentFolderById(id);
        Member member = memberService.findMemberByEmail(sharedDocumentFolder.getOwner());
        MemberInfo memberInfo = new MemberInfo(member);
        if(sharedDocumentFolder == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        GetSharedDocumentFolderResponse response = new GetSharedDocumentFolderResponse();
        response.setSharedDocumentFolder(sharedDocumentFolder);
        response.setMemberInfo(memberInfo);
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
        SharedDocumentFolder sharedDocumentFolder = sharedDocumentFolderService.findSharedDocumentFolderById(id);
        logger.debug("uuid : " + sharedDocumentFolder.getUuid());
        if(sharedDocumentFolder == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        else{
            String presignedURL = sharedDocumentFolderService.getPresignedDownloadURL(sharedDocumentFolder.getOwner(), sharedDocumentFolder.getName(), sharedDocumentFolder.getUuid());
            GetSharedDocumentFolderDownloadURLResponse response = new GetSharedDocumentFolderDownloadURLResponse();
            response.setPresignedURL(presignedURL);
            return ResponseEntity.ok(response);

        }


    }

    @PostMapping
    public ResponseEntity<CreateSharedDocumentFolderResponse> createSharedDocumentFolders(HttpServletRequest request, @RequestBody CreateSharedDocumentFolderRequest requestBody) {
        String email = authService.extractEmailFromToken(request);
        CreateSharedDocumentFolderResponse response = new CreateSharedDocumentFolderResponse();

        try{
            SharedDocumentFolder folder = sharedDocumentFolderService.createSharedFolder(email, requestBody.getName()+".zip", requestBody.getDocuments());
            response.setSharedDocumentFolder(folder);

        }catch(IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteSharedDocumentFolderResponse> deleteSharedDocumentFolders(HttpServletRequest request, @PathVariable String id){
        String email = authService.extractEmailFromToken(request);
        boolean result = sharedDocumentFolderService.deleteSharedDocument(email, id);
        DeleteSharedDocumentFolderResponse response = new DeleteSharedDocumentFolderResponse();
        response.setResult(result);
        if(result == true){
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
