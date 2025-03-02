package com.dev.vault96.service.shared;

import com.dev.vault96.controller.message.shared.GetSharedDocumentFolderDownloadURLResponse;
import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import com.dev.vault96.repository.shared.SharedDocumentFolderRepository;
import com.dev.vault96.service.document.DocumentService;
import com.dev.vault96.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedDocumentFolderService {

    private final SharedDocumentFolderRepository sharedDocumentFolderRepository;
    private final DocumentService documentService;
    private final S3Service s3Service;

    public SharedDocumentFolder findSharedDocumentFolderById(String id) {
        return sharedDocumentFolderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No Such Folder: " + id));
    }

    public List<SharedDocumentFolder> findSharedDocumentFoldersByOwner(String email) {
        return sharedDocumentFolderRepository.findSharedDocumentFoldersByOwner(email);
    }

    public SharedDocumentFolder findSharedDocumentFolderByIdAndOwner(String id, String email){
        Optional<SharedDocumentFolder> sharedDocumentFolder = sharedDocumentFolderRepository.findSharedDocumentFolderByIdAndOwner(id, email);
        if(sharedDocumentFolder.isPresent()){
            return sharedDocumentFolder.get();
        }
        else{
            return null;
        }
    }

    public SharedDocumentFolder createSharedFolder(String owner, String name, List<String> documentIds) throws IOException {
        String folderId = UUID.randomUUID().toString();

        // 1️⃣ 문서 검증
        List<Document> validDocuments = documentService.validateAndGetDocumentsByOwner(documentIds, owner);

        // 2️⃣ S3에서 문서 내용 가져오기
        List<byte[]> documentContents = validDocuments.stream()
                .map(doc -> s3Service.getDocumentContent(owner, doc.getId())) // ✅ getContent() 대신 S3에서 불러옴
                .collect(Collectors.toList());

        List<String> fileNames = validDocuments.stream().map(Document::getName).collect(Collectors.toList());

        // 3️⃣ ZIP 압축 후 S3 업로드
        String s3Url = s3Service.uploadDocumentsAsZip(owner, folderId, documentContents, fileNames);

        SharedDocumentFolder sharedFolder = SharedDocumentFolder.builder()
                .id(folderId)
                .owner(owner)
                .name(name)
                .documents(documentIds)
                .expiredAt(java.time.LocalDateTime.now().plusDays(2))
                .build();

        return sharedDocumentFolderRepository.save(sharedFolder);
    }

    public String getPresignedDownloadURL(String owner, String fileName, String id ){
        String presignedURL = s3Service.getSharedDocumentFolderPresignedDownloadUrl(owner, fileName, id).toString();
        return presignedURL;
    }

    public boolean deleteSharedDocument(String owner, String id){
        Optional<SharedDocumentFolder> folder = sharedDocumentFolderRepository.findSharedDocumentFolderByIdAndOwner(id, owner);
        if(folder.isPresent()){
            s3Service.deleteSharedDocumentFolder(owner, id);
            sharedDocumentFolderRepository.delete(folder.get());
            return true;
        }
        else{
            return false;
        }
    }
}
