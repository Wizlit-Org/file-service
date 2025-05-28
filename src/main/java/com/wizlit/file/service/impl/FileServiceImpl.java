package com.wizlit.file.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import jakarta.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.codec.multipart.FilePart;

import com.wizlit.file.exception.ApiException;
import com.wizlit.file.exception.ErrorCode;
import com.wizlit.file.model.domain.FileDto;
import com.wizlit.file.service.FileService;
import com.wizlit.file.service.S3Service;
import com.wizlit.file.service.manager.FileManager;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

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
                String fileExtension = null;
                
                // Extract file extension more safely
                int lastDotIndex = filename.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
                    fileExtension = filename.substring(lastDotIndex + 1).toLowerCase();
                }
                
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

                    // 2) check for existing by hash
                    return fileManager.findByHash(md5)
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
                                return Mono.just(new FileDto(savedFile));
                            })
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
            .flatMap(file -> fileManager.fileViewed(fileId)
                .thenReturn(file)
                .map(FileDto::new));
    }

}