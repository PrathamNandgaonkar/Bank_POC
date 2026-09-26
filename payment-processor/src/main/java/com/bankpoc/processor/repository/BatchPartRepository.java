package com.bankpoc.processor.repository;

import com.bankpoc.processor.model.entity.BatchPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchPartRepository extends JpaRepository<BatchPart, Long> {
    
    List<BatchPart> findByBatchId(String batchId);
    
    int countByBatchId(String batchId);
    
    boolean existsByBatchIdAndPartNumber(String batchId, int partNumber);
}
