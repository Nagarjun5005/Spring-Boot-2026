package com.interceptorLearning.interceptor.controller.annotationBased;


import com.interceptorLearning.interceptor.entity.annotationBased.RateLimited;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnnotationBasedRateLimiterController {

    @RateLimited(limit = 5,window = 60)
    @GetMapping("/login")
    public String login(){
        return "Login API";
    }

    @GetMapping("/public")
    public String publicApi() {
        return "No rate limit here";
    }
}
