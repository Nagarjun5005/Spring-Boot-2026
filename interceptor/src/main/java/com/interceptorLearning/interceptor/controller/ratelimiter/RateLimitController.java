package com.interceptorLearning.interceptor.controller.ratelimiter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class RateLimitController {

    @GetMapping("/test")
    public String test() {
        return "Request successful!";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from rate limited endpoint!";
    }
}
