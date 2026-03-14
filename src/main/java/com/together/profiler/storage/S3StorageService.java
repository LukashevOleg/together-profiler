package com.together.profiler.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * Сервис работы с S3-совместимым хранилищем.
 * В dev — MinIO, в prod — AWS S3.
 * Переключение через application.yml (storage.endpoint).
 */
@Slf4j
@Service
public class S3StorageService {

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

    @Value("${storage.presigned-url-ttl-minutes:60}")
    private int presignedTtlMinutes;

    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        var builder = S3Client.builder()
                .credentialsProvider(credentials)
                .region(Region.of(region));

        var presignerBuilder = S3Presigner.builder()
                .credentialsProvider(credentials)
                .region(Region.of(region));

        // Если задан кастомный endpoint — значит это MinIO (или localstack)
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true);  // MinIO требует path-style
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = builder.build();
        this.presigner = presignerBuilder.build();

        ensureBucketExists();
        log.info("S3StorageService initialized. Endpoint: {}, Bucket: {}", endpoint, bucket);
    }

    // ── Загрузить файл и вернуть S3-ключ (не URL) ───────────────────────────
    // Ключ затем используется в getPublicUrl() — это надёжнее чем разбирать URL обратно
    public String upload(MultipartFile file, String folder) throws IOException {
        String key = buildKey(folder, file.getOriginalFilename());

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        log.debug("Uploaded file to S3: {}", key);
        return key;  // возвращаем ключ, а не URL
    }

    // ── Получить presigned URL для загрузки прямо с фронта ─────────────────
    public PresignedUploadResult generatePresignedUploadUrl(String folder, String filename,
                                                            String contentType) {
        String key = buildKey(folder, filename);

        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedTtlMinutes))
                .putObjectRequest(r -> r
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                )
                .build();

        String uploadUrl = presigner.presignPutObject(presignRequest).url().toString();
        String publicUrl = buildPublicUrl(key);

        log.debug("Generated presigned URL for key: {}", key);
        return new PresignedUploadResult(key, uploadUrl, publicUrl);
    }

    // ── Удалить файл ────────────────────────────────────────────────────────
    public void delete(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            log.debug("Deleted S3 object: {}", s3Key);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", s3Key, e.getMessage());
        }
    }

    // ── Получить публичный URL по ключу ─────────────────────────────────────
    public String getPublicUrl(String s3Key) {
        return buildPublicUrl(s3Key);
    }

    // ── Извлечь S3-ключ из публичного URL ───────────────────────────────────
    // URL вида: http://localhost:9000/idea-hub/ideas/123/uuid.jpg
    public String extractKeyFromUrl(String url) {
        String prefix = endpoint + "/" + bucket + "/";
        return url.startsWith(prefix) ? url.substring(prefix.length()) : url;
    }

    // ── Внутренние хелперы ──────────────────────────────────────────────────
    private String buildKey(String folder, String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + ext;
    }

    private String buildPublicUrl(String key) {
        return endpoint + "/" + bucket + "/" + key;
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket: {}", bucket);
        }
    }

    public record PresignedUploadResult(String s3Key, String uploadUrl, String publicUrl) {}
}