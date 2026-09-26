package com.bankpoc.processor.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRegistryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_checksum", unique = true, nullable = false)
    private String fileChecksum;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "batch_id")
    private String batchId;

    @Builder.Default
    @Column(name = "status")
    private String status = "REGISTERED";

    @Builder.Default
    @Column(name = "registered_at")
    private LocalDateTime registeredAt = LocalDateTime.now();
}
