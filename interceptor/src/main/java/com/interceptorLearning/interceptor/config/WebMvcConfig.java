package com.interceptorLearning.interceptor.config;

import com.interceptorLearning.interceptor.config.annotationBased.RateLimitAnnotationInterceptor;
import com.interceptorLearning.interceptor.config.custom.RequestInterceptor;
import com.interceptorLearning.interceptor.config.ratelimiter.RateLimitGlobalInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestInterceptor requestInterceptor;
    private final RateLimitGlobalInterceptor rateLimitGlobalInterceptor;
    private final RateLimitAnnotationInterceptor rateLimitAnnotationInterceptor;


    public WebMvcConfig(RequestInterceptor requestInterceptor, RateLimitGlobalInterceptor rateLimitGlobalInterceptor, RateLimitAnnotationInterceptor rateLimitAnnotationInterceptor){
       this.requestInterceptor=requestInterceptor;
       this.rateLimitGlobalInterceptor = rateLimitGlobalInterceptor;
       this.rateLimitAnnotationInterceptor = rateLimitAnnotationInterceptor;
    }

    public void addInterceptors(InterceptorRegistry registry){
       registry.addInterceptor(requestInterceptor).addPathPatterns("/student/**");
       registry.addInterceptor(rateLimitGlobalInterceptor).addPathPatterns("/api/**");
       registry.addInterceptor(rateLimitAnnotationInterceptor);
    }



}
