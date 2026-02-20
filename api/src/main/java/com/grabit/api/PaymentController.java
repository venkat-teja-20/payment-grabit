package com.grabit.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/payment")
public class PaymentController {

    @GetMapping(value = "/test",produces = MediaType.APPLICATION_JSON_VALUE)
    public Object invokeTest(){
        return ResponseEntity.ok(Map.of("status","Success"));
    }
}
