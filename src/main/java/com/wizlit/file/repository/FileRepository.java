package com.wizlit.file.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.wizlit.file.entity.File;

import reactor.core.publisher.Mono;

public interface FileRepository extends ReactiveCrudRepository<File, String> {
    Mono<File> findByFileHash(String fileHash);
}
