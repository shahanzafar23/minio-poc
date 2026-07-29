package com.stc.minio.demo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PresignResponse {

    String presignedUrl;
    String objectKey;
    String bucket;
    String method;
    long expirySeconds;
    String publicDirectUrl;
    String note;
}
