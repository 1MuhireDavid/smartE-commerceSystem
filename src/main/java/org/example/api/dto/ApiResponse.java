package org.example.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard API response envelope returned by every endpoint.
 *
 * @param <T> payload type
 */
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Result of the operation", example = "success",
            allowableValues = {"success", "error"})
    private final String status;

    @Schema(description = "Human-readable description of the result",
            example = "Users retrieved successfully")
    private final String message;

    @Schema(description = "Response payload — null on error responses")
    private final T data;

    private ApiResponse(String status, String message, T data) {
        this.status  = status;
        this.message = message;
        this.data    = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("OK", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null);
    }

    public String getStatus()  { return status; }
    public String getMessage() { return message; }
    public T      getData()    { return data; }
}
