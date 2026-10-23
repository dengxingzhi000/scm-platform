package com.scmcloud.fulfillment.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TrackingDTO implements Serializable {

    private String fulfillmentNo;
    private String trackingNo;
    private String carrier;
    private String status;
    private String location;
    private String description;
    private LocalDateTime eventTime;
}
