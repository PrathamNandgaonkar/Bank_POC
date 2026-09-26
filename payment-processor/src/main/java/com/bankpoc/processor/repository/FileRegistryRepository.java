package com.bankpoc.processor.repository;

import com.bankpoc.processor.model.entity.FileRegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRegistryRepository extends JpaRepository<FileRegistryEntry, Long> {
    
    boolean existsByFileChecksum(String fileChecksum);
    
    List<FileRegistryEntry> findByBatchId(String batchId);
}
