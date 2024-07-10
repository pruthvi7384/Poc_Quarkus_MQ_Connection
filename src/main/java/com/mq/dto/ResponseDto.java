package com.mq.dto;

import lombok.Data;

@Data
public class ResponseDto {
    private String errorCode;
    private String errorDescription;
    private Object response;
    private Object mqResponseReceived;
}
