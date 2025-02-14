package com.dev.vault96.service.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Getter
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner; // ✅ Presigned URL 생성을 위한 Presigner 사용

    private static final String BUCKET_NAME = "vault96-bucket";
    private static final String TEMP_PREFIX = "/temp/document/";
    private static final String DOCS_PREFIX = "/documents/";


    public long getFileSize(String bucketName, String email, String name) {
        String key = DOCS_PREFIX + email + "/"+name;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            return headResponse.contentLength(); // ✅ 파일 크기 반환 (bytes)
        } catch (NoSuchKeyException e) {
            System.out.println("파일이 존재하지 않음: " + key);
            return -1; //
        }
    }

    public URL getPresignedUploadUrl(String email, String fileName) {
        String key = TEMP_PREFIX + email + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType("application/octet-stream")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url();
    }


    public void moveFileToDocuments(String email, String fileName) {
        String tempKey = TEMP_PREFIX + email + "/" + fileName;
        String finalKey = DOCS_PREFIX + email + "/" + fileName;

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(BUCKET_NAME)
                .sourceKey(tempKey)
                .destinationBucket(BUCKET_NAME)
                .destinationKey(finalKey)
                .build();

        s3Client.copyObject(copyRequest);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(tempKey)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    public void moveFile(String email, String srcName, String destName){
        String srcKey = TEMP_PREFIX + email + "/" + srcName;
        String destKey = DOCS_PREFIX + email + "/" + destName;

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(BUCKET_NAME)
                .sourceKey(srcKey)
                .destinationBucket(BUCKET_NAME)
                .destinationKey(destKey)
                .build();

        s3Client.copyObject(copyRequest);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(srcKey)
                .build();

        s3Client.deleteObject(deleteRequest);


    }

    public URL getPresignedDownloadUrl(String email, String fileName) {
        String key = DOCS_PREFIX + email + "/" + fileName; // ✅ 정식 저장 경로

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // ✅ Presigned URL 유효시간 설정 (10분)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public void uploadAndMoveFile(String email, String fileName) {
        moveFileToDocuments(email, fileName);
    }

}
