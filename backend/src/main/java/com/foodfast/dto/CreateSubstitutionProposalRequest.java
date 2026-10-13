package com.foodfast.dto;

public class CreateSubstitutionProposalRequest {
    public Long orderItemId;
    public String replacementProductId;
    public String reason;
    public Integer expiresInMinutes;
}
