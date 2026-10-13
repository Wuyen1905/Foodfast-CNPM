package com.foodfast.dto;

import com.foodfast.entity.CancellationReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CancelOrderRequest {

    @NotNull
    public CancellationReasonCode reasonCode;

    @Size(max = 500)
    public String detail;
}
