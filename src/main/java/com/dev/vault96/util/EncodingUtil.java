package com.dev.vault96.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Component
@RequiredArgsConstructor
public class EncodingUtil {
    public static String encodeFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, "UTF-8")
                    .replaceAll("\\+", "%20"); // 공백 처리
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("파일명 인코딩 실패", e);
        }
    }
}
