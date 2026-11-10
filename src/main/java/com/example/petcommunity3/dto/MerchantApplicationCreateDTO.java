package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class MerchantApplicationCreateDTO {
    private String merchantName;
    private String contactName;
    private String contactPhone;
    private String businessType;
    private String businessAddress;
    private String reason;
    private String attachmentName;
    private String attachmentUrl;
}
