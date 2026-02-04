package com.example.pproject.Config;

// ================================================================
// S3 설정 - 현재 비활성화 (주석처리)
// S3를 사용하려면 아래 주석을 해제하세요
// ================================================================

/*
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Bean
    public S3Client s3Client() {
        // 환경변수가 설정되어 있으면 해당 인증 정보 사용, 아니면 기본 자격 증명 체인 사용 (EC2 IAM Role 등)
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .build();
        }
        
        // EC2 IAM Role 또는 기본 자격 증명 체인 사용
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
*/

// S3 비활성화 - 빈 클래스로 대체
public class S3Config {
    // S3 기능 비활성화됨
}
