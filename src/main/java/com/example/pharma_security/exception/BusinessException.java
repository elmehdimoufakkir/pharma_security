package com.example.pharma_security.exception;

/** Une règle métier n'est pas respectée : aucune modification n'est validée. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
