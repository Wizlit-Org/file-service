package com.wizlit.file.model.domain;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wizlit.file.entity.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDto {
    private String fullName;
    private String fileId;
    private Long fileSize;
    private Long fileUploader;
    private Long fileCreatedTimestamp;
    private String fileType;
    private String fileExtension;

    public FileDto(File file) {
        this.fileId = file.getFileId();
        this.fileSize = file.getFileSize();
        this.fileUploader = file.getFileUploader();
        this.fileCreatedTimestamp = file.getFileCreatedTimestamp().toEpochMilli();
        this.fileType = file.getFileType();
        this.fileExtension = file.getFileExtension();
        this.fullName = file.getFileId() + "." + this.fileExtension;
    }
}