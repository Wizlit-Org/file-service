package com.wizlit.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    
    public String save(
        String bucket,
        String path,
        String fileName,
        Optional<Map<String, String>> optionalMetadata,
        InputStream inputStream,
        Long fileSize
    ) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket is required");
        }

        String key = (path == null || path.isBlank())
            ? fileName
            : String.format("%s/%s", path, fileName);

        Map<String, String> userMeta = optionalMetadata.orElse(Collections.emptyMap());

        PutObjectRequest putReq = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .metadata(userMeta)
            .build();

        s3Client.putObject(putReq, RequestBody.fromInputStream(inputStream, fileSize));
        
        return s3Client.utilities()
            .getUrl(builder -> builder.bucket(bucket).key(key))
            .toExternalForm();
    }

    public String generatePresignedUrl(String bucket, String path, String fileName, String contentType) {
        String key = (path == null || path.isBlank())
            ? fileName
            : String.format("%s/%s", path, fileName);

        GetObjectRequest getReq = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .responseContentType(contentType)
            .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .getObjectRequest(getReq)
            .build();

        return presigner.presignGetObject(presignReq)
            .url()
            .toString();
    }

    public byte[] download(String bucket, String key) {
        GetObjectRequest getReq = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        try (ResponseInputStream<GetObjectResponse> resp = s3Client.getObject(getReq)) {
            return IoUtils.toByteArray(resp);
        } catch (SdkException | IOException e) {
            throw new IllegalStateException("Failed to download file from S3", e);
        }
    }

    public void deleteFile(String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        } catch (SdkException e) {
            throw new IllegalStateException("Failed to delete file from S3", e);
        }
    }

    public void deleteFiles(String bucket, String pathName, List<String> urls) {
        List<ObjectIdentifier> objects = new ArrayList<>();
        for (String url : urls) {
            try {
                URI uri = new URI(url);
                String id = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
                objects.add(ObjectIdentifier.builder()
                    .key(pathName + "/" + id)
                    .build());
            } catch (URISyntaxException e) {
                // handle or log bad URL
            }
        }

        DeleteObjectsRequest delReq = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(Delete.builder()
                .objects(objects)
                .build())
            .build();

        try {
            s3Client.deleteObjects(delReq);
        } catch (SdkException e) {
            throw new IllegalStateException("Failed to delete files from S3", e);
        }
    }
}
