package com.grabit.config;

import com.grabit.enums.CommonErrors;
import com.grabit.exception.APIError;
import com.grabit.exception.CustomException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Log4j2
public class APIExceptionHandler {

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<APIError> handleCustomException(CustomException ex){
        return ResponseEntity
                .status(ex.getErrorObject().getHttpCode())
                .body(ex.getErrorObject().getErrorMsg());
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<APIError> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletResponse response){
        return ResponseEntity
                .status(ex.getBody().getStatus())
                .body(new APIError("INVALID_REQUEST_PATH","The requested resource "+ex.getResourcePath()+" does not exist"));
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIError> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletResponse response){
        return ResponseEntity
                .status(ex.getBody().getStatus())
                .body(new APIError("METHOD_NOT_ALLOWED",ex.getBody().getDetail()));
    }

    @ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<APIError> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, HttpServletResponse response) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new APIError("INVALID_MEDIA_TYPE", ex.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<APIError> handleException(Exception ex){
        log.error("Something went wrong : "+ex);
        return ResponseEntity
                .status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .body(new APIError(CommonErrors.SOMETHING_WENT_WRONG.toString(), ex.getMessage()));
    }
}
