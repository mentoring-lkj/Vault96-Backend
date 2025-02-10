package com.dev.vault96.service.member;

import com.dev.vault96.dto.document.DocumentDetail;
import com.dev.vault96.repository.document.DocumentDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentDetailService {
    private final DocumentDetailRepository documentDetailRepository;

    public DocumentDetail findDocumentDetailById(String id){
        Optional<DocumentDetail> documentDetail = documentDetailRepository.findDocumentDetailById(id);
        if(documentDetail.isPresent()) return documentDetail.get();
        else return null;
    }

    public List<DocumentDetail> findDocumentDetailsByDocumentId(String documentId){
        Optional<List<DocumentDetail>> documentDetails = documentDetailRepository.findDocumentDetailsByDocumentId(documentId);
        if(documentDetails.isPresent()) return documentDetails.get();
        else return null;
    }

    public DocumentDetail findDocumentDetailByDocumentIdAndVersion(String documentId, String version){
        Optional<DocumentDetail> documentDetail = documentDetailRepository.findDocumentDetailByDocumentIdAndVersion(documentId, version);
        if(documentDetail.isPresent()) return documentDetail.get();
        else return null;
    }

    public void save(DocumentDetail documentDetail){
        documentDetailRepository.save(documentDetail);
    }
}
