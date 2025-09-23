package com.tem.be.api.dto;

import lombok.Data;

@Data
public class ReviewActionDTO {
    private String batchId;
    private ActionType action;

    private String reviewedBy;
    private String rejectionReason;

    public enum ActionType {
        APPROVE,
        REJECT
    }
}
