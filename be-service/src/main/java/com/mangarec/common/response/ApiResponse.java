package com.mangarec.common.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class ApiResponse<T> implements Serializable {
    private int status;
    private String message;
    private T data;
}
