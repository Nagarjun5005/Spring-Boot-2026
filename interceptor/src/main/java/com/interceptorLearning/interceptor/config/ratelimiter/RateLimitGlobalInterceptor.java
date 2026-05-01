package com.interceptorLearning.interceptor.config.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RateLimitGlobalInterceptor implements HandlerInterceptor {
    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW = 60000; // 1 minute

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();


    @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String clientIp=getClientIp(request);
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());

        if(counter.isRateLimitExceeded()){
            response.setStatus(429);
            response.getWriter().write("Rate Limit exceeded.Try Again Later.");
            return false;
        }
        counter.increment();
        return true;

    }

    private String getClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if(header==null){
            return request.getRemoteAddr();
        }
        return header.split(",")[0];

    }


    private class RequestCounter {
        private int count = 0;
        private long windowStart = System.currentTimeMillis();

        public synchronized void increment() {
            long now = System.currentTimeMillis();
            if (now - windowStart > TIME_WINDOW) {
                count = 0;
                windowStart = now;
            }
            count++;
        }

        public synchronized boolean isRateLimitExceeded() {
            long now = System.currentTimeMillis();
            if (now - windowStart > TIME_WINDOW) {
                count = 0;
                windowStart = now;
                return false;
            }
            return count >= MAX_REQUESTS;
        }
    }
}
