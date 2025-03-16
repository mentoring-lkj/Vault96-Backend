package com.dev.vault96.repository.shared;

import com.dev.vault96.entity.document.Document;
import com.dev.vault96.entity.shared.SharedDocumentFolder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedDocumentFolderRepository extends MongoRepository<SharedDocumentFolder, String> {

    Optional<SharedDocumentFolder> findSharedDocumentFolderById(String id);
    List<SharedDocumentFolder> findSharedDocumentFoldersByOwner(String email);
    Optional<SharedDocumentFolder> findSharedDocumentFolderByIdAndOwner(String id, String owner);
    List<SharedDocumentFolder> findByOwnerAndDocumentsContaining(String owner, Document document);

    Optional<SharedDocumentFolder> findSharedDocumentFolderByOwnerAndName(String owner, String name);

    void deleteAllByIdIn(List<String> ids);

}
