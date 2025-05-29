package com.wizlit.file.service;

import org.springframework.http.codec.multipart.FilePart;

import com.wizlit.file.model.domain.FileDto;

import reactor.core.publisher.Mono;

public interface FileService {
    Mono<FileDto> upload(FilePart filePart, Long uploaderUserId);
    Mono<FileDto> getFileMetadataMarkViewed(String fileId);
}
