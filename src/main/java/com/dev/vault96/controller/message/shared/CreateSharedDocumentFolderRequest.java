package com.dev.vault96.controller.message.shared;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateSharedDocumentFolderRequest {
    public String name;
    public List<String> documents;
}
