package com.bankpoc.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AITimelineEvent {
    private LocalDateTime timestamp;
    private String event;
    private String details;
}
