package com.stc.minio.demo.service;

import com.stc.minio.demo.dto.PresignRequest;
import com.stc.minio.demo.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedDeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PresignService {

    private final S3Presigner presigner;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    @Value("${minio.presign.default-expiry-seconds}")
    private long defaultExpirySeconds;

    @Value("${minio.presign.public-expiry-seconds}")
    private long publicExpirySeconds;

    @Value("${minio.presign.max-expiry-seconds}")
    private long maxExpirySeconds;

    public PresignResponse presignPut(PresignRequest request, String prefix, boolean isPublic) {
        String key = normalizeKey(prefix, request.getObjectKey());
        long expiry = resolveExpiry(request.getExpirySeconds(), isPublic);

        PutObjectRequest.Builder objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            objectRequest.contentType(request.getContentType());
        }

        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(expiry))
                        .putObjectRequest(objectRequest.build())
                        .build());

        return buildResponse(presigned.url().toString(), key, "PUT", expiry, isPublic,
                isPublic
                        ? "After upload, anonymous GET works at publicDirectUrl."
                        : "Private object — use presigned GET to download.");
    }

    public PresignResponse presignGet(PresignRequest request, String prefix, boolean isPublic) {
        String key = normalizeKey(prefix, request.getObjectKey());
        long expiry = resolveExpiry(request.getExpirySeconds(), isPublic);

        PresignedGetObjectRequest presigned = presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(expiry))
                        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                        .build());

        return buildResponse(presigned.url().toString(), key, "GET", expiry, isPublic,
                isPublic ? "Public prefix: direct URL may work without signature." : "Private: presigned URL required.");
    }

    public PresignResponse presignDelete(PresignRequest request, String prefix, boolean isPublic) {
        String key = normalizeKey(prefix, request.getObjectKey());
        long expiry = resolveExpiry(request.getExpirySeconds(), isPublic);

        PresignedDeleteObjectRequest presigned = presigner.presignDeleteObject(
                DeleteObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(expiry))
                        .deleteObjectRequest(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
                        .build());

        return buildResponse(presigned.url().toString(), key, "DELETE", expiry, isPublic,
                "Use HTTP DELETE against presignedUrl before expiry.");
    }

    private PresignResponse buildResponse(String url, String key, String method, long expiry, boolean isPublic, String note) {
        return PresignResponse.builder()
                .presignedUrl(url)
                .objectKey(key)
                .bucket(bucket)
                .method(method)
                .expirySeconds(expiry)
                .publicDirectUrl(isPublic ? buildDirectUrl(key) : null)
                .note(note)
                .build();
    }

    private String normalizeKey(String prefix, String objectKey) {
        String cleaned = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return cleaned.startsWith(prefix) ? cleaned : prefix + cleaned;
    }

    private long resolveExpiry(Long requested, boolean isPublic) {
        long fallback = isPublic ? publicExpirySeconds : defaultExpirySeconds;
        if (requested == null || requested <= 0) {
            return fallback;
        }
        return Math.min(requested, maxExpirySeconds);
    }

    private String buildDirectUrl(String key) {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + "/" + bucket + "/" + key;
    }
}
