package com.dev.vault96.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


public class FileContentTypeUtil {

    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();

    static {
        // ✅ 문서 파일
        CONTENT_TYPES.put("pdf", "application/pdf");
        CONTENT_TYPES.put("doc", "application/msword");
        CONTENT_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        CONTENT_TYPES.put("txt", "text/plain");
        CONTENT_TYPES.put("rtf", "application/rtf");
        CONTENT_TYPES.put("csv", "text/csv");
        CONTENT_TYPES.put("xls", "application/vnd.ms-excel");
        CONTENT_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        CONTENT_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        CONTENT_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        // ✅ 이미지 파일
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("bmp", "image/bmp");
        CONTENT_TYPES.put("svg", "image/svg+xml");
        CONTENT_TYPES.put("webp", "image/webp");

        // ✅ 기타 (압축파일, JSON, XML)
        CONTENT_TYPES.put("zip", "application/zip");
        CONTENT_TYPES.put("tar", "application/x-tar");
        CONTENT_TYPES.put("rar", "application/x-rar-compressed");
        CONTENT_TYPES.put("json", "application/json");
        CONTENT_TYPES.put("xml", "application/xml");
    }

    /**
     * 파일 확장자에 따른 S3 Content-Type 반환
     * @param extension 파일 확장자 (ex: "pdf", "jpg")
     * @return S3 Content-Type 값
     */
    public static String getContentType(String extension) {
        return CONTENT_TYPES.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }
}
