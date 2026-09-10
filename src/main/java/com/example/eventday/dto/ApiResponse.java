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
public class ApiResponse<T> {

    private String msg;
    private int status;

    // Memastikan field 'data' selalu muncul di JSON (termasuk bernilai null)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private T data;

    // ==========================================
    // 1. CORE FACTORY METHODS
    // ==========================================

    public static <T> ApiResponse<T> success(String msg, T data) {
        return ApiResponse.<T>builder()
                .msg(msg)
                .status(200)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String msg) {
        return ApiResponse.<T>builder()
                .msg(msg)
                .status(status)
                .data(null)
                .build();
    }

    // ==========================================
    // 2. SUCCESS HELPERS (Status 2xx)
    // ==========================================

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return success(msg, data);
    }

    public static <T> ApiResponse<T> ok(String msg) {
        return success(msg, null);
    }

    public static <T> ApiResponse<T> created(String msg, T data) {
        return ApiResponse.<T>builder()
                .msg(msg)
                .status(201)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(String msg) {
        return ApiResponse.<T>builder()
                .msg(msg)
                .status(201)
                .data(null)
                .build();
    }

    // ==========================================
    // 3. CLIENT ERROR HELPERS (Status 4xx)
    // ==========================================

    public static <T> ApiResponse<T> badRequest(String msg) {
        return error(400, msg);
    }

    public static <T> ApiResponse<T> unauthorized(String msg) {
        return error(401, msg);
    }

    public static <T> ApiResponse<T> forbidden(String msg) {
        return error(403, msg);
    }

    public static <T> ApiResponse<T> notFound(String msg) {
        return error(404, msg);
    }

    // ==========================================
    // 4. SERVER ERROR HELPERS (Status 5xx)
    // ==========================================

    public static <T> ApiResponse<T> internalError(String msg) {
        return error(500, msg);
    }
}