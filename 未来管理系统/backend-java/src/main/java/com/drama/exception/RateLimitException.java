package com.drama.exception;

/** 限流拒绝异常，被 GlobalExceptionHandler 映射为 HTTP 429 + 业务 code 429。 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
