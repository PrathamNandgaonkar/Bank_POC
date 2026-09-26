package com.bankpoc.processor.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchIngestRequest {
    private String batchId;
    private List<String> filePaths;
    private int totalParts;
    private String priority;
}
