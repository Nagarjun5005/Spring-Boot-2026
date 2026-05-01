# Rate Limit Interceptor - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [How It Works](#how-it-works)
3. [Code Breakdown](#code-breakdown)
4. [Configuration](#configuration)
5. [Testing](#testing)
6. [Production Considerations](#production-considerations)
7. [Common Issues & Solutions](#common-issues--solutions)

---

## Overview

The `RateLimitInterceptor` is a Spring Boot component that implements API rate limiting to prevent abuse and ensure fair usage of your application's resources. It limits the number of requests a client can make within a specific time window.

### Key Features
- ✅ Per-client IP rate limiting
- ✅ Configurable request limits and time windows
- ✅ Thread-safe implementation
- ✅ Automatic window reset
- ✅ Support for proxy scenarios (X-Forwarded-For header)

### Default Configuration
- **Max Requests**: 10 requests
- **Time Window**: 60 seconds (1 minute)
- **Response Code**: 429 (Too Many Requests)

---

## How It Works

### Request Flow Diagram

```
Client Request
     ↓
RateLimitInterceptor.preHandle()
     ↓
Extract Client IP (consider X-Forwarded-For)
     ↓
Get/Create RequestCounter for this IP
     ↓
Check if rate limit exceeded?
     ↓
  YES → Return 429 (Request Blocked)
     ↓
  NO → Increment counter → Allow Request
     ↓
Continue to Controller
```

### Sliding Window Algorithm

The interceptor uses a **sliding time window** approach:

```
Time Window: 60 seconds, Max Requests: 10

Example Timeline:
┌────────────────────────────────────────────────────────┐
│  Window Start: 10:00:00                                │
│  Current Time: 10:00:45                                │
│  Requests Made: 8                                      │
│  Status: ✅ Allow (8 < 10)                             │
└────────────────────────────────────────────────────────┘

After 60 seconds:
┌────────────────────────────────────────────────────────┐
│  Window Start: 10:01:00 (auto-reset)                   │
│  Current Time: 10:01:05                                │
│  Requests Made: 0 (counter reset)                      │
│  Status: ✅ Allow (fresh window)                       │
└────────────────────────────────────────────────────────┘
```

---

## Code Breakdown

### 1. Class Structure

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
```

**Explanation:**
- `@Component`: Marks this as a Spring-managed bean (automatically detected and registered)
- `implements HandlerInterceptor`: Makes this class intercept HTTP requests

---

### 2. Configuration Constants

```java
private static final int MAX_REQUESTS = 10;
private static final long TIME_WINDOW = 60000; // 1 minute in milliseconds
```

**Explanation:**
- `MAX_REQUESTS`: Maximum number of allowed requests per client
- `TIME_WINDOW`: Duration in milliseconds (60000ms = 60 seconds = 1 minute)
- `static final`: These are constants (unchangeable at runtime)

**To customize:**
```java
private static final int MAX_REQUESTS = 100;      // Allow 100 requests
private static final long TIME_WINDOW = 300000;   // Per 5 minutes
```

---

### 3. Request Counter Storage

```java
private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
```

**Explanation:**
- `Map<String, RequestCounter>`: Stores IP address → request counter mapping
- `ConcurrentHashMap`: Thread-safe map (multiple requests can access simultaneously)
- `String` (key): Client IP address
- `RequestCounter` (value): Tracks request count and window start time

**Example Data:**
```
requestCounts = {
    "192.168.1.100" → RequestCounter(count=5, windowStart=1234567890000),
    "192.168.1.101" → RequestCounter(count=10, windowStart=1234567891000),
    "10.0.0.50"     → RequestCounter(count=2, windowStart=1234567892000)
}
```

---

### 4. preHandle() Method - The Main Logic

```java
@Override
public boolean preHandle(HttpServletRequest request, 
                        HttpServletResponse response, 
                        Object handler) throws Exception {
```

**Explanation:**
- `@Override`: Overrides the interface method
- Called **before** the controller method executes
- **Return `true`**: Allow request to proceed
- **Return `false`**: Block request (stop processing)

---

### 5. Getting Client IP Address

```java
String clientIp = getClientIP(request);
RequestCounter counter = requestCounts.computeIfAbsent(
    clientIp, 
    k -> new RequestCounter()
);
```

**Step-by-step:**

1. **Extract IP**: `getClientIP(request)` returns the client's IP address
2. **Get or Create Counter**: `computeIfAbsent()` does:
    - If IP exists in map → return existing counter
    - If IP doesn't exist → create new counter and add to map

**Example:**
```java
// First request from 192.168.1.100
clientIp = "192.168.1.100"
counter = NEW RequestCounter() // Created
requestCounts.put("192.168.1.100", counter)

// Second request from same IP
clientIp = "192.168.1.100"
counter = EXISTING RequestCounter() // Retrieved
```

---

### 6. Rate Limit Check

```java
if (counter.isRateLimitExceeded()) {
    response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
    response.getWriter().write("Rate limit exceeded. Try again later.");
    return false; // BLOCK REQUEST
}

counter.increment(); // INCREMENT COUNTER
return true; // ALLOW REQUEST
```

**Flow:**
```
Is rate limit exceeded?
     ↓
   YES → Set HTTP 429 status
       → Write error message
       → Return false (block)
     ↓
   NO → Increment counter
      → Return true (allow)
```

---

### 7. Getting Client IP (Proxy-Aware)

```java
private String getClientIP(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null) {
        return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0];
}
```

**Why X-Forwarded-For?**

When your app is behind a proxy/load balancer:

```
Client (Real IP: 203.0.113.50)
    ↓
Load Balancer (IP: 10.0.0.1)
    ↓
Your App sees: 10.0.0.1 ❌ (Load balancer IP, not client)
```

**X-Forwarded-For header** contains the real client IP:
```
X-Forwarded-For: 203.0.113.50, 10.0.0.1, 192.168.1.1
                 ↑
                 Real client IP (we want this)
```

**Code Logic:**
```java
// Case 1: Direct connection (no proxy)
xfHeader = null
return request.getRemoteAddr() // e.g., "203.0.113.50"

// Case 2: Behind proxy
xfHeader = "203.0.113.50, 10.0.0.1"
return "203.0.113.50" // First IP in the list
```

---

### 8. RequestCounter Inner Class

```java
private class RequestCounter {
    private int count = 0;
    private long windowStart = System.currentTimeMillis();
```

**Variables:**
- `count`: Number of requests made in current window
- `windowStart`: Timestamp when the current window started (in milliseconds)

**Example:**
```java
// Window starts at 10:00:00
windowStart = 1700000000000L (timestamp)
count = 0

// After 3 requests
count = 3
windowStart = 1700000000000L (same window)

// After 60+ seconds
count = 0 (reset)
windowStart = 1700000060000L (new window)
```

---

### 9. Increment Method

```java
public synchronized void increment() {
    long now = System.currentTimeMillis();
    if (now - windowStart > TIME_WINDOW) {
        count = 0;
        windowStart = now;
    }
    count++;
}
```

**Step-by-step execution:**

```java
// Example 1: Within time window
now = 1700000030000 (30 seconds after window start)
windowStart = 1700000000000
now - windowStart = 30000ms (30 seconds)
30000 > 60000? NO
→ Just increment: count++

// Example 2: Window expired
now = 1700000065000 (65 seconds after window start)
windowStart = 1700000000000
now - windowStart = 65000ms (65 seconds)
65000 > 60000? YES
→ Reset: count = 0, windowStart = 1700000065000
→ Then increment: count++
```

**Why `synchronized`?**
- Multiple threads (requests) might call this simultaneously
- `synchronized` ensures only one thread modifies the counter at a time
- Prevents race conditions

**Without synchronized (PROBLEM):**
```
Thread 1: Read count = 5
Thread 2: Read count = 5
Thread 1: Write count = 6
Thread 2: Write count = 6 ❌ (Should be 7!)
```

**With synchronized (CORRECT):**
```
Thread 1: Lock → Read count = 5 → Write count = 6 → Unlock
Thread 2: Wait → Lock → Read count = 6 → Write count = 7 → Unlock ✅
```

---

### 10. Rate Limit Check Method

```java
public synchronized boolean isRateLimitExceeded() {
    long now = System.currentTimeMillis();
    if (now - windowStart > TIME_WINDOW) {
        count = 0;
        windowStart = now;
        return false; // Fresh window, not exceeded
    }
    return count >= MAX_REQUESTS; // Check if limit reached
}
```

**Logic Flow:**

```java
// Scenario 1: Window expired
now = 1700000070000
windowStart = 1700000000000
now - windowStart = 70000ms > 60000ms
→ Reset counter
→ Return false (allow request)

// Scenario 2: Within window, under limit
count = 7
MAX_REQUESTS = 10
7 >= 10? NO
→ Return false (allow request)

// Scenario 3: Within window, at/over limit
count = 10
MAX_REQUESTS = 10
10 >= 10? YES
→ Return true (block request)
```

---

## Configuration

### Step 1: Create the Interceptor
The code you provided is complete. Save it as:
```
src/main/java/com/example/interceptor/RateLimitInterceptor.java
```

### Step 2: Register the Interceptor

```java
package com.example.config;

import com.example.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");  // Apply to all /api/* endpoints
                // .excludePathPatterns("/api/public/**");  // Optional: exclude paths
    }
}
```

### Step 3: Test Controller

```java
package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {
    
    @GetMapping("/test")
    public String test() {
        return "Request successful!";
    }
}
```

---

## Testing

### Manual Testing with cURL

```bash
# Test 1: Send 10 requests (should all succeed)
for i in {1..10}; do
    echo "Request $i:"
    curl -i http://localhost:8080/api/test
    echo ""
done

# Test 2: 11th request should fail with 429
echo "Request 11 (should fail):"
curl -i http://localhost:8080/api/test
```

**Expected Output:**

```
Requests 1-10:
HTTP/1.1 200 OK
Content-Type: text/plain
Request successful!

Request 11:
HTTP/1.1 429 Too Many Requests
Content-Type: text/plain
Rate limit exceeded. Try again later.
```

### Automated Test with JUnit

```java
package com.example.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitInterceptorTest {
    
    private RateLimitInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
    }
    
    @Test
    void testRateLimitNotExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // First 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            boolean result = interceptor.preHandle(request, response, new Object());
            assertTrue(result, "Request " + (i + 1) + " should be allowed");
        }
    }
    
    @Test
    void testRateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 10 requests (all should succeed)
        for (int i = 0; i < 10; i++) {
            interceptor.preHandle(request, response, new Object());
        }
        
        // 11th request should fail
        response = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(request, response, new Object());
        
        assertFalse(result, "11th request should be blocked");
        assertEquals(429, response.getStatus());
    }
    
    @Test
    void testDifferentIPsTrackedSeparately() throws Exception {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("192.168.1.100");
        
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("192.168.1.101");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 10 requests from IP1
        for (int i = 0; i < 10; i++) {
            interceptor.preHandle(request1, response, new Object());
        }
        
        // Request from IP2 should still succeed (different counter)
        boolean result = interceptor.preHandle(request2, response, new Object());
        assertTrue(result, "Different IP should have its own limit");
    }
    
    @Test
    void testXForwardedForHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean result = interceptor.preHandle(request, response, new Object());
        
        assertTrue(result);
        // The interceptor should use 203.0.113.50 as the client IP
    }
}
```

---

## Production Considerations

### 1. Memory Leaks - The Problem

The current implementation stores all IP addresses **forever**:

```java
private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
```

**Issue:**
- Every unique IP creates a new entry
- Map grows indefinitely
- Eventually causes **OutOfMemoryError**

**Example:**
```
After 1 day: 10,000 unique IPs → 10,000 map entries
After 1 week: 70,000 unique IPs → 70,000 map entries
After 1 month: 300,000+ entries → MEMORY LEAK! 💥
```

### Solution 1: Scheduled Cleanup

```java
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    // Run cleanup every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > TIME_WINDOW
        );
        System.out.println("Cleanup: Removed expired entries. Map size: " + requestCounts.size());
    }
    
    // ... rest of the code
}
```

**Enable scheduling in main class:**
```java
@SpringBootApplication
@EnableScheduling  // Add this
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Solution 2: Use Redis (Recommended for Production)

```java
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    
    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW_SECONDS = 60;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String clientIp = getClientIP(request);
        String key = "rate_limit:" + clientIp;
        
        Integer count = redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            // First request - set counter with expiry
            redisTemplate.opsForValue().set(key, 1, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
            return true;
        }
        
        if (count >= MAX_REQUESTS) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Rate limit exceeded. Try again later.");
            return false;
        }
        
        // Increment counter
        redisTemplate.opsForValue().increment(key);
        return true;
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
```

**Benefits of Redis:**
- ✅ Automatic expiry (no memory leaks)
- ✅ Works across multiple server instances
- ✅ Persistent (survives server restarts)
- ✅ Much faster for high traffic

---

### 2. Distributed Systems

**Problem:** Current solution only works on **single server**

```
Server 1: Client makes 10 requests → Blocked
    ↓
Load Balancer
    ↓
Server 2: Same client makes 10 MORE requests → Allowed ❌
```

**Solution:** Use Redis (shared state across servers)

---

### 3. Make Configuration Externalized

```java
// application.properties
rate.limit.max.requests=100
rate.limit.time.window=60000

// Interceptor
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Value("${rate.limit.max.requests}")
    private int maxRequests;
    
    @Value("${rate.limit.time.window}")
    private long timeWindow;
    
    // Use maxRequests and timeWindow instead of constants
}
```

---

### 4. Better Error Response

```java
if (counter.isRateLimitExceeded()) {
    response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
    response.setContentType("application/json");
    
    String jsonResponse = String.format(
        "{\"error\":\"Rate limit exceeded\",\"retry_after\":%d}",
        TIME_WINDOW / 1000
    );
    
    response.getWriter().write(jsonResponse);
    return false;
}
```

**Response:**
```json
{
  "error": "Rate limit exceeded",
  "retry_after": 60
}
```

---

## Common Issues & Solutions

### Issue 1: All Clients Share Same Limit

**Symptom:** User A makes 10 requests, User B gets blocked

**Cause:** Not extracting client IP correctly

**Solution:** Verify `getClientIP()` is working:
```java
String clientIp = getClientIP(request);
System.out.println("Client IP: " + clientIp);  // Debug log
```

---

### Issue 2: Rate Limit Not Working Behind Proxy

**Symptom:** All requests show same IP (e.g., `10.0.0.1`)

**Cause:** Proxy not setting `X-Forwarded-For` header

**Solution:** Configure proxy to send header:

**Nginx:**
```nginx
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

**Spring Boot (if using embedded tomcat):**
```properties
server.tomcat.remoteip.remote-ip-header=x-forwarded-for
```

---

### Issue 3: Rate Limit Resets Unexpectedly

**Symptom:** Counter resets before time window expires

**Cause:** Server restarts (in-memory map is lost)

**Solution:** Use Redis or database for persistence

---

### Issue 4: Thread Safety Issues

**Symptom:** Inconsistent counter values

**Cause:** Missing `synchronized` keyword

**Solution:** Already handled in the code - both methods are `synchronized`

---

## Performance Metrics

### Memory Usage

```
Per IP entry: ~100 bytes
10,000 IPs: ~1 MB
100,000 IPs: ~10 MB
1,000,000 IPs: ~100 MB
```

**Without cleanup:** Can grow to GB over time

### Processing Time

```
ConcurrentHashMap lookup: ~O(1) - nanoseconds
Synchronized method: ~microseconds
Total overhead: < 1ms per request
```

---

## Summary

### What This Interceptor Does:
1. ✅ Intercepts every HTTP request
2. ✅ Extracts client IP (proxy-aware)
3. ✅ Tracks request count per IP
4. ✅ Blocks requests exceeding limit
5. ✅ Auto-resets after time window

### Key Concepts:
- **ConcurrentHashMap**: Thread-safe IP tracking
- **synchronized**: Prevents race conditions
- **Sliding Window**: Time-based limit reset
- **X-Forwarded-For**: Real client IP detection

### Production Checklist:
- [ ] Add scheduled cleanup for memory
- [ ] Use Redis for distributed systems
- [ ] Externalize configuration
- [ ] Add proper logging
- [ ] Implement rate limit headers
- [ ] Monitor memory usage

---

This interceptor provides basic rate limiting suitable for development and small applications. For production with high traffic, consider using Redis-based solutions or dedicated API gateways like Kong, Nginx, or AWS API Gateway.