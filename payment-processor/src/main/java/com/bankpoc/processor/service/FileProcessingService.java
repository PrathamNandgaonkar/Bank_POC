package com.bankpoc.processor.service;

import com.bankpoc.processor.dto.CSVTransactionRecord;
import com.bankpoc.processor.exception.FileProcessingException;
import com.bankpoc.processor.model.entity.FileRegistryEntry;
import com.bankpoc.processor.repository.FileRegistryRepository;
import com.bankpoc.processor.util.ChecksumUtil;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingService {

    private final FileRegistryRepository fileRegistryRepository;
    private final ChecksumUtil checksumUtil;

    public List<CSVTransactionRecord> parseCSVFile(String filePath) {
        log.info("Parsing CSV file: {}", filePath);
        try (Reader reader = new FileReader(filePath)) {
            return new CsvToBeanBuilder<CSVTransactionRecord>(reader)
                    .withType(CSVTransactionRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        } catch (Exception e) {
            log.error("Error parsing CSV file: {}", filePath, e);
            throw new FileProcessingException("Failed to parse CSV file: " + filePath, e);
        }
    }

    public ValidationResult validateCSVStructure(String filePath) {
        List<String> errors = new ArrayList<>();
        // Basic check for headers could be done here.
        // For simplicity, we consider it valid if it can be read.
        return new ValidationResult(true, errors);
    }

    public String computeFileChecksum(String filePath) {
        try {
            return checksumUtil.computeChecksum(filePath);
        } catch (Exception e) {
            log.error("Error computing checksum for file: {}", filePath, e);
            throw new FileProcessingException("Failed to compute checksum", e);
        }
    }

    public boolean isDuplicateFile(String checksum) {
        return fileRegistryRepository.existsByFileChecksum(checksum);
    }

    public void registerFile(String fileName, String checksum, long size, String batchId) {
        FileRegistryEntry entry = FileRegistryEntry.builder()
                .fileName(fileName)
                .fileChecksum(checksum)
                .fileSizeBytes(size)
                .batchId(batchId)
                .build();
        fileRegistryRepository.save(entry);
        log.info("Registered file {} for batch {}", fileName, batchId);
    }

    public record ValidationResult(boolean isValid, List<String> errors) {}
}
