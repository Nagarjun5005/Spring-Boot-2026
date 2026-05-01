package com.interceptorLearning.interceptor.config.annotationBased;

import com.interceptorLearning.interceptor.entity.annotationBased.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RateLimitAnnotationInterceptor implements HandlerInterceptor {

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();


    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        // 🔍 Check if annotation is present
        RateLimited rateLimited = method.getMethodAnnotation(RateLimited.class);

        if (rateLimited == null) {
            return true; // no rate limiting
        }

        String clientIp = getClientIp(request);
        String key=clientIp+":"+request.getRequestURI();
        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter(rateLimited.limit(), rateLimited.window()));

        if(counter.isRateLimitExceeded()){
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded. Try again later.");
            return false;
        }
        counter.increment();
        return true;

    }

    private String getClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header == null) {
            return request.getRemoteAddr();
        }
        return header.split(",")[0];
    }


    private static class RequestCounter {
        private int count = 0;
        private long windowStart;
        private final int limit;
        private final long windowMillis;

        public RequestCounter(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowMillis = windowSeconds * 1000L;
            this.windowStart = System.currentTimeMillis();
        }

        public synchronized void increment() {
            long now = System.currentTimeMillis();

            if (now - windowStart > windowMillis) {
                count = 0;
                windowStart = now;
            }

            count++;
        }

        public synchronized boolean isRateLimitExceeded() {
            long now = System.currentTimeMillis();

            if (now - windowStart > windowMillis) {
                count = 0;
                windowStart = now;
                return false;
            }

            return count >= limit;
        }
    }

}
