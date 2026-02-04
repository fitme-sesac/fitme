package com.example.pproject.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
// S3 관련 import 주석처리
// import software.amazon.awssdk.core.sync.RequestBody;
// import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
// import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    // S3Client 주석처리 - S3 비활성화
    // private final S3Client s3Client;

    @Value("${tempFolder:C:/PProject/uploads/}")
    private String uploadPath;

    @Value("${cloud.aws.s3.bucket:}")
    private String bucketName;

    @Value("${cloud.aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${cloud.aws.cloudfront.domain:}")
    private String cloudfrontDomain;

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param subDirectory 하위 디렉토리 (예: "logos", "resumes")
     * @return 저장된 파일의 URL
     */
    public String uploadFile(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 원본 파일명
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }

        // 확장자 추출
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        // 허용된 이미지 확장자 검증
        String lowerExt = extension.toLowerCase();
        if (!lowerExt.equals(".png") && !lowerExt.equals(".jpg") && 
            !lowerExt.equals(".jpeg") && !lowerExt.equals(".gif") && !lowerExt.equals(".webp")) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다. (png, jpg, jpeg, gif, webp만 가능)");
        }

        // UUID로 고유 파일명 생성
        String savedFilename = UUID.randomUUID().toString() + extension;

        // S3 비활성화 - 항상 로컬 저장 사용
        // if (s3Enabled && bucketName != null && !bucketName.isBlank()) {
        //     return uploadToS3(file, subDirectory, savedFilename);
        // } else {
        //     return uploadToLocal(file, subDirectory, savedFilename);
        // }
        return uploadToLocal(file, subDirectory, savedFilename);
    }

    /**
     * S3에 파일 업로드 - 현재 비활성화
     */
    /*
    private String uploadToS3(MultipartFile file, String subDirectory, String savedFilename) throws IOException {
        String s3Key = subDirectory + "/" + savedFilename;

        // Content-Type 설정
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        log.info("S3 파일 업로드 완료: s3://{}/{}", bucketName, s3Key);

        // CloudFront 도메인이 있으면 CloudFront URL 반환, 없으면 S3 URL 반환
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            return "https://" + cloudfrontDomain + "/" + s3Key;
        } else {
            return "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
        }
    }
    */

    /**
     * 로컬에 파일 업로드
     */
    private String uploadToLocal(MultipartFile file, String subDirectory, String savedFilename) throws IOException {
        // 저장 디렉토리 생성
        Path directory = Paths.get(uploadPath, subDirectory);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        // 파일 저장
        Path filePath = directory.resolve(savedFilename);
        file.transferTo(filePath.toFile());

        log.info("로컬 파일 저장 완료: {}", filePath);

        // 웹에서 접근 가능한 URL 경로 반환
        return "/images/" + subDirectory + "/" + savedFilename;
    }

    /**
     * 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // S3 비활성화 - 로컬 파일만 삭제
            // if (s3Enabled && (fileUrl.contains(".s3.amazonaws.com/") || 
            //         (cloudfrontDomain != null && fileUrl.contains(cloudfrontDomain)))) {
            //     deleteFromS3(fileUrl);
            // } else 
            if (fileUrl.startsWith("/images/")) {
                deleteFromLocal(fileUrl);
            }
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", fileUrl, e);
        }
    }

    /**
     * S3에서 파일 삭제 - 현재 비활성화
     */
    /*
    private void deleteFromS3(String fileUrl) {
        // S3 키 추출
        String s3Key;
        if (fileUrl.contains(".s3.amazonaws.com/")) {
            s3Key = fileUrl.substring(fileUrl.indexOf(".s3.amazonaws.com/") + 18);
        } else if (cloudfrontDomain != null && fileUrl.contains(cloudfrontDomain)) {
            s3Key = fileUrl.substring(fileUrl.indexOf(cloudfrontDomain) + cloudfrontDomain.length() + 1);
        } else {
            log.warn("S3 키를 추출할 수 없음: {}", fileUrl);
            return;
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("S3 파일 삭제 완료: s3://{}/{}", bucketName, s3Key);
    }
    */

    /**
     * 로컬에서 파일 삭제
     */
    private void deleteFromLocal(String fileUrl) throws IOException {
        String relativePath = fileUrl.replace("/images/", "");
        Path filePath = Paths.get(uploadPath, relativePath);
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("로컬 파일 삭제 완료: {}", filePath);
        }
    }
}
