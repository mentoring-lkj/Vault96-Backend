package com.dev.vault96.service.s3;

import com.dev.vault96.util.DocumentUtil;
import com.dev.vault96.util.EncodingUtil;
import com.dev.vault96.util.FileContentTypeUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Getter
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final DocumentUtil documentUtil;
    private final EncodingUtil encodingUtil;

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private static final String BUCKET_NAME = "vault96-bucket";
    private static final String TEMP_PREFIX = "/temp/document/";
    private static final String DOCS_PREFIX = "/documents/";
    private static final String SHARED_PREFIX = "/shared/";

    public boolean doesFileExist(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;  // 파일 존재함
        } catch (NoSuchKeyException e) {
            return false; // 파일 없음
        }
    }

    public long getTempFileSize(String email, String fileId){
        String key = TEMP_PREFIX + email + "/" + fileId;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();
            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            return headResponse.contentLength();
        } catch (NoSuchKeyException e) {
            System.out.println("파일이 존재하지 않음: " + key);
            return -1;
        }

    }

    public long getDocsFileSize(String email, String fileId) {
        String key = DOCS_PREFIX + email + "/" + fileId;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();
            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            return headResponse.contentLength();
        } catch (NoSuchKeyException e) {
            System.out.println("파일이 존재하지 않음: " + key);
            return -1;
        }
    }

    public URL getPresignedUploadUrl(String email, String fileName) {
        String key = TEMP_PREFIX + email + "/" + fileName;
        String extension = documentUtil.getFileExtension(fileName);
        String contentType = FileContentTypeUtil.getContentType(extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url();
    }

    public boolean moveFileToDocuments(String email, String documentName, String documentId) {

        String srcKey = TEMP_PREFIX + email + "/" + documentName;
        String destKey = DOCS_PREFIX + email + "/" + documentId;
        logger.debug(srcKey);
        if (!doesFileExist(srcKey)) {
            logger.debug("no such srcKey : ", srcKey);
            return false;
        }

        try {
            logger.debug("try to copy upload ");
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(BUCKET_NAME)
                    .sourceKey(srcKey)
                    .destinationBucket(BUCKET_NAME)
                    .destinationKey(destKey)
                    .build();

            s3Client.copyObject(copyRequest);
            logger.debug("copy object end ");

            deleteFileByKey(srcKey); // 원본 파일 삭제
            logger.debug("deleted srckey file ");

            return true;
        } catch (Exception e) {
            logger.error("S3 파일 이동 실패: {}", e.getMessage());
            return false;
        }
    }

    public void deleteDocument(String email, String fileId) {
        String fileKey = DOCS_PREFIX + email + "/" + fileId;
        deleteFileByKey(fileKey);
    }

    public void deleteSharedDocumentFolder(String email, String fileId) {
        String fileKey = SHARED_PREFIX + email + "/" + fileId;
        if(!doesFileExist(fileKey)){
            logger.debug("no such folder");
        }
        deleteFileByKey(fileKey);
    }

    private void deleteFileByKey(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 실패: " + key, e);
        }
    }

    public URL getDocumentPresignedDownloadUrl(String email, String fileName, String fileId) {
        return generatePresignedDownloadUrl(DOCS_PREFIX + email + "/" + fileId, fileName);
    }

    public URL getSharedDocumentFolderPresignedDownloadUrl(String email, String fileName, String fileId) {
        return generatePresignedDownloadUrl(SHARED_PREFIX + email + "/" + fileId, fileName);
    }

    public URL generatePresignedDownloadUrl(String key, String fileName) {
        if (!doesFileExist(key)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다: " + key);
        }

        String encodedFileName = EncodingUtil.encodeFileName(fileName) ;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .responseContentDisposition("attachment; filename=\"" + encodedFileName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public String uploadFolder(String owner, String folderId, List<byte[]> documentContents, List<String> fileNames) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {
            for (int i = 0; i < documentContents.size(); i++) {
                ZipEntry zipEntry = new ZipEntry(fileNames.get(i));
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(documentContents.get(i));
                zipOutputStream.closeEntry();
            }
        }

        byte[] zipBytes = byteArrayOutputStream.toByteArray();
        String s3Key = SHARED_PREFIX + owner + "/" + folderId ;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .contentType("application/zip")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipBytes));

        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build()).toString();
    }

    public byte[] getDocumentContent(String owner, String documentId) {
        String key = DOCS_PREFIX + owner + "/" + documentId;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);

        return objectBytes.asByteArray();
    }

}
