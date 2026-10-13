package com.foodfast.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class CreateReviewRequest {

    @Min(1)
    @Max(5)
    public int rating;

    @Size(max = 1000)
    public String comment;
}
