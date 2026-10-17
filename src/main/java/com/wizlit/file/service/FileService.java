package com.wizlit.file.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.codec.multipart.FilePart;

import com.wizlit.file.model.domain.FileDto;
import com.wizlit.file.model.domain.UserQuotaResponse;

import reactor.core.publisher.Mono;

public interface FileService {
    Mono<FileDto> upload(FilePart filePart, Long uploaderUserId);
    Mono<FileDto> getFileMetadataMarkViewed(String fileId);
    Mono<Void> attachFiles(Long memoId, List<String> fileIds);
    Mono<Void> deleteFilesByMemoId(Long memoId);
    Mono<Integer> cleanupOrphanFiles(Instant cutoff);
    Mono<UserQuotaResponse> getUserQuota(Long userId);
}
