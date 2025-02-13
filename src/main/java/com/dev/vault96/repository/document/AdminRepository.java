package com.dev.vault96.repository.document;

import com.dev.vault96.entity.document.Admin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {
    Optional<Admin> findAdminByEmail(String email);
}
