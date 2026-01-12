package com.example.pproject.Config;

//import com.amazonaws.auth.AWSStaticCredentialsProvider;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcConfig {
//    @Value("${cloud.aws.credentials.accessKey}")
//    private String accessKey; //공개키
//    @Value("${cloud.aws.credentials.secretKey}")
//    private String secretKey; // ="TNSyBf811oZPGabmCAP2LYQEGGt/cuHfSgVUHKvO"; //비밀키
//    @Value("${cloud.aws.region.static}")
//    private String region; //지역
//
//    @Bean
//    public AmazonS3Client amazonS3Client() {
//        //클라이언트에서 서버(AWS S3)에 로그인처리
//        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
//        //지정한 지역에 공개키와 비밀키를 이용해서 접속
//        return (AmazonS3Client) AmazonS3ClientBuilder.standard()
//                .withRegion(region)
//                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
//                .build();
//    }
}