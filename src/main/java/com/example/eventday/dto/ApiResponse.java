package com.example.eventday.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String msg;
    private int status;
    private T data;

    public static <T> ApiResponse<T> success(String msg, T data) {
        return ApiResponse.<T>builder().msg(msg).status(200).data(data).build();
    }

    public static <T> ApiResponse<T> success(String msg) {
        return ApiResponse.<T>builder().msg(msg).status(200).data(null).build();
    }

    public static <T> ApiResponse<T> created(String msg, T data) {
        return ApiResponse.<T>builder().msg(msg).status(201).data(data).build();
    }

    public static <T> ApiResponse<T> error(String msg, int status) {
        return ApiResponse.<T>builder().msg(msg).status(status).data(null).build();
    }
}
