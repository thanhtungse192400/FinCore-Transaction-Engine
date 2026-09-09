package com.fincore.fincorebe.Exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends ApiException {
    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
