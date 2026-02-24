package com.grabit.enums;

import lombok.Getter;

public enum CommonErrors {
    AUTHENTICATION_FAILED("Error decoding signature"),
    AUTHENTICATION_EXPIRED("Signature has expired"),
    AUTHENTICATION_ERROR("Authentication is required"),
    AUTHENTICATION_REQUIRED("User is not authenticated"),
    SOMETHING_WENT_WRONG("Something went wrong"),
    FORBIDDEN("You do not have the permission to access this resource");

    @Getter
    private String message;

    CommonErrors(String msg){
        this.message=msg;
    }
}
