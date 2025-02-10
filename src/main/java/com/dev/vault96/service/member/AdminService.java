package com.dev.vault96.service.member;

import com.dev.vault96.dto.document.Admin;
import com.dev.vault96.repository.document.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    public Admin findAdminByEmail(String email){
        Optional<Admin> admin = adminRepository.findAdminByEmail(email);
        if(admin.isPresent()) return admin.get();
        else return null;
    }

}
