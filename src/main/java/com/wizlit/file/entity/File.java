package com.wizlit.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("file")
public class File {

    /**
     * auto remove files...
     * - FileMemo empty + fileWasUsed + fileUpdatedTimestamp after .1~3 Years
     * - fileWasUsed false + fileUpdatedTimestamp after 1~7 days
     * */

    @Id
    @Column("file_id")
    private String fileId;

    @Column("file_size")
    private Long fileSize;
    
    @Column("file_uploader")
    private Long fileUploader;

    @Column("file_created_timestamp")
    private Instant fileCreatedTimestamp;

    @Column("file_type")
    private String fileType;

    @Column("file_extension")
    private String fileExtension;

    @Column("file_hash")
    private String fileHash;
}