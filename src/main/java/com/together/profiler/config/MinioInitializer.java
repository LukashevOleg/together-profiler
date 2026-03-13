package com.together.profiler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

/**
 * Запускается при старте приложения.
 * Создаёт бакет если не существует и выставляет политику публичного чтения.
 *
 * Структура бакета:
 *   idea-hub/
 *     ideas/
 *       {ideaId}/          ← фото конкретной идеи
 *         uuid.jpg
 */
@Slf4j
@Component
public class MinioInitializer implements ApplicationRunner {

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.access-key}")
    private String accessKey;

    @Value("${storage.secret-key}")
    private String secretKey;

    @Value("${storage.region}")
    private String region;

    @Override
    public void run(ApplicationArguments args) {
        S3Client client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)
                .build();

        createBucketIfNotExists(client);
        applyPublicReadPolicy(client);
    }

    private void createBucketIfNotExists(S3Client client) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket '{}' created", bucket);
        }
    }

    /**
     * Выставляем bucket policy: GET на все объекты — публичный.
     * Это нужно чтобы фото отдавались напрямую без авторизации.
     * POST/DELETE — только с credentials (только сервис).
     */
    private void applyPublicReadPolicy(S3Client client) {
        String policy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Sid": "PublicReadGetObject",
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": "s3:GetObject",
                  "Resource": "arn:aws:s3:::%s/*"
                }
              ]
            }
            """.formatted(bucket);

        try {
            client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucket)
                    .policy(policy)
                    .build());
            log.info("Public read policy applied to bucket '{}'", bucket);
        } catch (Exception e) {
            log.warn("Could not apply bucket policy (ok in AWS, required in MinIO): {}", e.getMessage());
        }
    }
}
