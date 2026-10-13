package com.foodfast.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateRestaurantOperationsRequest {

    public Boolean acceptingOrders;

    @Size(max = 300)
    public String pauseReason;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Giờ mở cửa phải có dạng HH:mm")
    public String openingTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Giờ đóng cửa phải có dạng HH:mm")
    public String closingTime;
}
