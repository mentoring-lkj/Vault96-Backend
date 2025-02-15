package com.dev.vault96.controller.message.document;


import lombok.Getter;

import java.util.List;

@Getter
public class CreateDocumentRequestBody {

    private String name;
    private List<String> tags;

}
