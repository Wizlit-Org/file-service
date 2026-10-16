package com.wizlit.file.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import jakarta.xml.bind.DatatypeConverter;

import com.wizlit.file.entity.File;
import com.wizlit.file.entity.UserStorageQuota;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.codec.multipart.FilePart;

import com.wizlit.file.exception.ApiException;
import com.wizlit.file.exception.ErrorCode;
import com.wizlit.file.exception.QuotaExceededException;
import com.wizlit.file.model.domain.FileDto;
import com.wizlit.file.model.domain.UserQuotaResponse;
import com.wizlit.file.service.FileService;
import com.wizlit.file.service.S3Service;
import com.wizlit.file.service.manager.FileManager;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long DEFAULT_MAX_QUOTA_BYTES = 104857600L; // 100MB

    private final S3Service s3Service;
    private final FileManager fileManager;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */
    
    @Transactional
    @Override
    public Mono<FileDto> upload(FilePart filePart, Long uploaderUserId) {
        return Mono.<Path>fromCallable(() -> Files.createTempFile("upload-", "-" + filePart.filename()))
            .flatMap(tmpPath -> filePart.transferTo(tmpPath).thenReturn(tmpPath))
            .flatMap(tmpPath -> {
                MediaType contentType = filePart.headers().getContentType();
                String filename = filePart.filename();
                int lastDotIndex = filename.lastIndexOf('.');
                String fileExtension = (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) 
                    ? filename.substring(lastDotIndex + 1).toLowerCase() 
                    : null;
                
                if (contentType == null || fileExtension == null) {
                    throw new ApiException(ErrorCode.INVALID_FILE_FORMAT);
                }

                // 1) compute MD5 hash and real size
                try (InputStream in = Files.newInputStream(tmpPath)) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] buf = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = in.read(buf)) != -1) {
                        md.update(buf, 0, bytesRead);
                    }

                    String md5 = DatatypeConverter.printHexBinary(md.digest());
                    long size = Files.size(tmpPath);
                    String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

                    // 2) quota pre-validation
                    return fileManager.getUserStorageQuota(uploaderUserId, yearMonth)
                        .defaultIfEmpty(UserStorageQuota.builder()
                            .userId(uploaderUserId)
                            .yearMonth(yearMonth)
                            .usedBytes(0L)
                            .maxBytes(DEFAULT_MAX_QUOTA_BYTES)
                            .build())
                        .flatMap(quota -> {
                            long used = quota.getUsedBytes() != null ? quota.getUsedBytes() : 0L;
                            long max = quota.getMaxBytes() != null ? quota.getMaxBytes() : DEFAULT_MAX_QUOTA_BYTES;
                            if (used + size > max) {
                                return Mono.error(new QuotaExceededException(used, max, size));
                            }
                            return Mono.just(max);
                        })
                        .flatMap(currentMax ->
                            fileManager.findByHash(md5)
                    .flatMap(existing ->
                        // already stored: return DTO immediately
                        Mono.just(new FileDto(existing))
                    )
                    .switchIfEmpty(
                        // new file: insert metadata + upload to S3 + return DTO
                        fileManager.insertFileMetadata(
                            size,
                            contentType.toString(),
                            fileExtension,
                            md5,
                            uploaderUserId
                        )
                        .flatMap(savedFile -> {
                            try (InputStream in2 = Files.newInputStream(tmpPath)) {
                                s3Service.save(
                                        bucket,
                                        null,
                                        savedFile.getFileId() + "." + savedFile.getFileExtension(),
                                        Optional.empty(),
                                        in2,
                                        size
                                    );
                                } catch (IOException ex) {
                                    return Mono.error(new IllegalStateException("S3 upload failed", ex));
                                }
                                return fileManager.upsertUserStorageQuota(uploaderUserId, yearMonth, size, currentMax)
                                    .thenReturn(new FileDto(savedFile));
                            })
                    )
                )
                    .doFinally(signal -> {
                        try { Files.deleteIfExists(tmpPath); }
                        catch (IOException ignored) {}
                    });
                } catch (IOException | NoSuchAlgorithmException e) {
                    return Mono.error(new IllegalStateException("Failed to process temp file", e));
                }
            });
    }
    
    @Override
    public Mono<FileDto> getFileMetadataMarkViewed(String fileId) {
        return fileManager.getFileMetadata(fileId)
            .switchIfEmpty(Mono.error(new ApiException(ErrorCode.FILE_NOT_FOUND, fileId)))
            .flatMap(file -> fileManager.fileViewed(fileId)
                .thenReturn(file)
                .map(FileDto::new));
    }

    @Transactional
    @Override
    public Mono<Void> attachFiles(Long memoId, List<String> fileIds) {
        if (memoId == null || fileIds == null || fileIds.isEmpty()) {
            return Mono.empty();
        }
        return fileManager.attachFiles(memoId, fileIds).then();
    }

    @Transactional
    @Override
    public Mono<Void> deleteFilesByMemoId(Long memoId) {
        if (memoId == null) {
            return Mono.empty();
        }
        return fileManager.getFilesByMemoId(memoId)
            .collectList()
            .flatMap(files -> {
                if (files.isEmpty()) {
                    return Mono.empty();
                }
                for (File file : files) {
                    try {
                        String key = file.getFileId() + "." + file.getFileExtension();
                        s3Service.deleteFile(bucket, key);
                    } catch (Exception ignored) {
                    }
                }
                return fileManager.deleteFilesByMemoId(memoId).then();
            });
    }

    @Transactional
    @Override
    public Mono<Integer> cleanupOrphanFiles(Instant cutoff) {
        Instant effectiveCutoff = cutoff != null ? cutoff : Instant.now().minus(java.time.Duration.ofHours(24));
        return fileManager.getPendingFilesBefore(effectiveCutoff)
            .collectList()
            .flatMap(files -> {
                if (files.isEmpty()) {
                    return Mono.just(0);
                }
                List<String> fileIdsToDelete = new java.util.ArrayList<>();
                for (File file : files) {
                    try {
                        String key = file.getFileId() + "." + file.getFileExtension();
                        s3Service.deleteFile(bucket, key);
                    } catch (Exception ignored) {
                    }
                    fileIdsToDelete.add(file.getFileId());
                }
                return fileManager.deleteFilesByIds(fileIdsToDelete);
            });
    }

    @Override
    public Mono<UserQuotaResponse> getUserQuota(Long userId) {
        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return fileManager.getUserStorageQuota(userId, yearMonth)
            .defaultIfEmpty(UserStorageQuota.builder()
                .userId(userId)
                .yearMonth(yearMonth)
                .usedBytes(0L)
                .maxBytes(DEFAULT_MAX_QUOTA_BYTES)
                .build())
            .map(quota -> {
                long used = quota.getUsedBytes() != null ? quota.getUsedBytes() : 0L;
                long max = quota.getMaxBytes() != null ? quota.getMaxBytes() : DEFAULT_MAX_QUOTA_BYTES;
                long remaining = Math.max(0L, max - used);
                return UserQuotaResponse.builder()
                    .userId(userId)
                    .yearMonth(yearMonth)
                    .usedBytes(used)
                    .maxBytes(max)
                    .remainingBytes(remaining)
                    .build();
            });
    }

}