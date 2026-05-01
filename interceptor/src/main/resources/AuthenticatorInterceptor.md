# Authentication Interceptor - Spring Boot

A comprehensive guide to implementing JWT-based authentication using Spring Boot interceptors.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [How It Works](#how-it-works)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Testing](#testing)
- [API Endpoints](#api-endpoints)
- [Common Issues](#common-issues)
- [Production Recommendations](#production-recommendations)

---

## Overview

The **Authentication Interceptor** validates JWT tokens on incoming HTTP requests, ensuring only authenticated users can access protected endpoints.

### What Problem Does It Solve?

Without an interceptor, you'd need to add authentication logic to **every controller method**:

```java
// ❌ Bad: Repeated authentication code
@GetMapping("/profile")
public String getProfile(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null) {
        throw new UnauthorizedException();
    }
    // ... validate token ...
    // ... actual business logic ...
}
```

With an interceptor, authentication happens **automatically**:

```java
// ✅ Good: Clean controller code
@GetMapping("/profile")
public String getProfile(HttpServletRequest request) {
    String username = (String) request.getAttribute("username");
    // ... actual business logic only ...
}
```

---

## Features

✅ **Automatic Token Validation** - Validates JWT tokens before reaching controllers  
✅ **Public Endpoint Support** - Exclude login/register endpoints  
✅ **CORS Preflight Handling** - Skips OPTIONS requests  
✅ **User Context Injection** - Makes user info available in controllers  
✅ **Clean Error Messages** - Returns proper 401 JSON responses  
✅ **Flexible Configuration** - Easy to add/remove protected paths

---

## How It Works

### Request Flow

```
Client Request
     ↓
CORS Preflight Check (OPTIONS) → Allow
     ↓
Public Endpoint Check → Allow
     ↓
Extract Authorization Header
     ↓
Header Missing? → 401 Unauthorized
     ↓
Invalid Format? → 401 Unauthorized
     ↓
Validate Token
     ↓
Token Invalid? → 401 Unauthorized
     ↓
Extract User Info → Add to Request Attributes
     ↓
Continue to Controller ✅
```

### Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  Request: GET /api/user/profile                             │
│  Header: Authorization: Bearer xyz123                       │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  AuthenticationInterceptor.preHandle()                      │
│  1. Check if OPTIONS request → Skip                         │
│  2. Check if public endpoint → Skip                         │
│  3. Extract token from header                               │
│  4. Validate token                                          │
│  5. Extract username                                        │
│  6. Add to request attributes                               │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  Controller Method                                          │
│  - Access username via request.getAttribute("username")     │
│  - Execute business logic                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Installation

### Step 1: Add Dependencies

Add to `pom.xml` (for Maven):

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

Or `build.gradle` (for Gradle):

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

### Step 2: Create Project Structure

```
src/main/java/com/example/
├── interceptor/
│   └── AuthenticationInterceptor.java
├── config/
│   └── AuthConfig.java
└── controller/
    └── AuthTestController.java
```

### Step 3: Copy the Files

Copy the three provided files:
- `AuthenticationInterceptor.java` → `src/main/java/com/example/interceptor/`
- `AuthConfig.java` → `src/main/java/com/example/config/`
- `AuthTestController.java` → `src/main/java/com/example/controller/`

---

## Configuration

### Basic Configuration

The `AuthConfig.java` file configures which endpoints require authentication:

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns("/api/**")              // Protect all /api endpoints
            .excludePathPatterns(
                "/api/auth/login",                   // Allow login
                "/api/auth/register",                // Allow registration
                "/api/public/**"                     // Allow all public endpoints
            );
}
```

### Customizing Protected Paths

#### Protect Specific Endpoints Only

```java
registry.addInterceptor(authenticationInterceptor)
        .addPathPatterns(
            "/api/user/**",
            "/api/admin/**",
            "/api/orders/**"
        );
```

#### Protect Everything Except Public

```java
registry.addInterceptor(authenticationInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/**",        // All auth endpoints
            "/api/public/**",      // All public endpoints
            "/api/health",         // Health check
            "/api/docs/**"         // API documentation
        );
```

### Multiple Interceptors

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // 1. CORS (first)
    registry.addInterceptor(corsInterceptor).order(1);
    
    // 2. Authentication
    registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**")
            .order(2);
    
    // 3. Authorization (after authentication)
    registry.addInterceptor(authorizationInterceptor)
            .addPathPatterns("/api/admin/**")
            .order(3);
}
```

---

## Usage

### In Controllers

Once authenticated, user information is available via request attributes:

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        // Get username set by interceptor
        String username = (String) request.getAttribute("username");
        String token = (String) request.getAttribute("token");
        
        // Use the username to fetch user data
        User user = userService.findByUsername(username);
        
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestBody UserUpdateRequest updateRequest,
            HttpServletRequest request) {
        
        String username = (String) request.getAttribute("username");
        
        userService.updateUser(username, updateRequest);
        
        return ResponseEntity.ok("Profile updated successfully");
    }
}
```

### Custom Validation Logic

Modify `isValidToken()` to add real JWT validation:

```java
private boolean isValidToken(String token) {
    try {
        // Example using jjwt library
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        
        // Check expiration
        if (claims.getExpiration().before(new Date())) {
            return false;
        }
        
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

---

## Testing

### Using Postman

#### Test 1: Access Public Endpoint (No Authentication)

**Request:**
```
GET http://localhost:8080/api/public/info
```

**Expected Response:**
```
Status: 200 OK
Body: "This is public information - no auth needed"
```

---

#### Test 2: Login to Get Token

**Request:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**Expected Response:**
```json
{
  "token": "dummy-jwt-token-12345",
  "message": "Login successful"
}
```

---

#### Test 3: Access Protected Endpoint WITHOUT Token

**Request:**
```
GET http://localhost:8080/api/user/profile
```

**Expected Response:**
```
Status: 401 Unauthorized

{
  "error": "Missing Authorization header"
}
```

---

#### Test 4: Access Protected Endpoint WITH Valid Token

**Request:**
```
GET http://localhost:8080/api/user/profile
Authorization: Bearer dummy-jwt-token-12345
```

**Expected Response:**
```
Status: 200 OK
Body: "User Profile for: user@example.com"
```

---

#### Test 5: Invalid Token Format

**Request:**
```
GET http://localhost:8080/api/user/dashboard
Authorization: invalid-token-format
```

**Expected Response:**
```
Status: 401 Unauthorized

{
  "error": "Invalid Authorization format. Use: Bearer <token>"
}
```

---

#### Test 6: Invalid/Expired Token

**Request:**
```
GET http://localhost:8080/api/user/dashboard
Authorization: Bearer invalid-or-expired-token
```

**Expected Response:**
```
Status: 401 Unauthorized

{
  "error": "Invalid or expired token"
}
```

---

### Using cURL

```bash
# Test 1: Public endpoint
curl http://localhost:8080/api/public/info

# Test 2: Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# Test 3: Protected endpoint without token
curl http://localhost:8080/api/user/profile

# Test 4: Protected endpoint with token
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer dummy-jwt-token-12345"
```

---

### Automated Testing with JUnit

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationInterceptorTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testPublicEndpoint_NoAuth_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/public/info"))
                .andExpect(status().isOk());
    }
    
    @Test
    void testProtectedEndpoint_NoAuth_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testProtectedEndpoint_WithAuth_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer dummy-jwt-token-12345"))
                .andExpect(status().isOk());
    }
    
    @Test
    void testProtectedEndpoint_InvalidFormat_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "invalid-format"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/public/info` | Get public information |
| POST | `/api/auth/login` | User login |
| POST | `/api/auth/register` | User registration |

### Protected Endpoints (Authentication Required)

| Method | Endpoint | Description | Required Header |
|--------|----------|-------------|-----------------|
| GET | `/api/user/profile` | Get user profile | `Authorization: Bearer <token>` |
| GET | `/api/user/dashboard` | Get user dashboard | `Authorization: Bearer <token>` |
| POST | `/api/user/update` | Update user data | `Authorization: Bearer <token>` |

---

## Common Issues

### Issue 1: Token Not Being Validated

**Symptom:** All requests pass through even without token

**Cause:** Interceptor not registered properly

**Solution:**
```java
// Make sure AuthConfig has @Configuration annotation
@Configuration  // ← This is required!
public class AuthConfig implements WebMvcConfigurer {
    // ...
}
```

---

### Issue 2: CORS Preflight Requests Failing

**Symptom:** Browser shows CORS errors, OPTIONS requests return 401

**Cause:** Interceptor blocking OPTIONS requests

**Solution:** Already handled in the code:
```java
if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
    return true; // Allow OPTIONS requests
}
```

---

### Issue 3: Public Endpoints Still Require Auth

**Symptom:** Login endpoint returns 401

**Cause:** Path pattern not matching correctly

**Solution:** Check your path patterns:
```java
// Make sure paths match exactly
.excludePathPatterns("/api/auth/login")  // Correct
.excludePathPatterns("/auth/login")      // Won't match /api/auth/login
```

---

### Issue 4: Can't Access Username in Controller

**Symptom:** `request.getAttribute("username")` returns null

**Cause:** Token validation or extraction failed silently

**Solution:** Add logging to debug:
```java
private String extractUsername(String token) {
    String username = "user@example.com"; // Your logic here
    System.out.println("Extracted username: " + username);
    return username;
}
```

---

## Production Recommendations

### 1. Use Real JWT Library

Replace the dummy token validation with a proper JWT library:

**Add Dependency:**
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

**Update Token Validation:**
```java
import io.jsonwebtoken.*;

private static final String SECRET_KEY = "your-secret-key-here";

private boolean isValidToken(String token) {
    try {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getExpiration().after(new Date());
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}

private String extractUsername(String token) {
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(SECRET_KEY)
            .build()
            .parseClaimsJws(token)
            .getBody();
    
    return claims.getSubject();
}
```

---

### 2. Add Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null) {
            logger.warn("Missing Authorization header for: {}", request.getRequestURI());
            // ... return 401
        }
        
        logger.info("Authenticated user: {} for endpoint: {}", 
                   username, request.getRequestURI());
        
        return true;
    }
}
```

---

### 3. Externalize Configuration

**application.properties:**
```properties
# JWT Configuration
jwt.secret=your-secret-key-here-change-in-production
jwt.expiration=86400000
jwt.header=Authorization
jwt.prefix=Bearer 
```

**In Interceptor:**
```java
@Value("${jwt.secret}")
private String secretKey;

@Value("${jwt.expiration}")
private long expiration;
```

---

### 4. Add Role-Based Authorization

```java
@Override
public boolean preHandle(HttpServletRequest request, 
                        HttpServletResponse response, 
                        Object handler) throws Exception {
    
    // ... existing token validation ...
    
    // Extract roles from token
    List<String> roles = extractRoles(token);
    request.setAttribute("roles", roles);
    
    // Check if user has required role for this endpoint
    if (request.getRequestURI().startsWith("/api/admin/")) {
        if (!roles.contains("ADMIN")) {
            response.setStatus(403); // Forbidden
            response.getWriter().write("{\"error\":\"Insufficient permissions\"}");
            return false;
        }
    }
    
    return true;
}
```

---

### 5. Handle Token Refresh

```java
@Override
public boolean preHandle(HttpServletRequest request, 
                        HttpServletResponse response, 
                        Object handler) throws Exception {
    
    // ... existing validation ...
    
    // Check if token is about to expire (within 5 minutes)
    if (isTokenExpiringSoon(token)) {
        String newToken = refreshToken(token);
        response.setHeader("X-New-Token", newToken);
    }
    
    return true;
}
```

---

### 6. Add Rate Limiting

Combine with the Rate Limit Interceptor:

```java
@Configuration
public class SecurityConfig implements WebMvcConfigurer {
    
    @Autowired
    private AuthenticationInterceptor authInterceptor;
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. Rate limiting (first)
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
        
        // 2. Authentication (after rate limiting)
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**")
                .order(2);
    }
}
```

---

## Security Best Practices

1. **Never log tokens** - They're sensitive credentials
2. **Use HTTPS** - Tokens can be intercepted over HTTP
3. **Set token expiration** - Short-lived tokens are safer
4. **Rotate secrets** - Change JWT secret keys periodically
5. **Validate token signature** - Always verify JWT signature
6. **Check token expiration** - Reject expired tokens
7. **Use strong secrets** - Minimum 256-bit key for HMAC
8. **Implement token blacklisting** - For logout functionality
9. **Add request origin validation** - Prevent CSRF attacks
10. **Monitor failed auth attempts** - Detect brute force attacks

---

## Summary

### What This Interceptor Does:
✅ Validates JWT tokens automatically  
✅ Blocks unauthorized requests  
✅ Injects user context into requests  
✅ Handles CORS preflight requests  
✅ Supports public endpoints  
✅ Returns clean error messages

### Key Points:
- Interceptors run **before** controller methods
- Return `true` to allow, `false` to block
- Use `request.setAttribute()` to pass data to controllers
- Configure paths in `WebMvcConfigurer`
- Always validate tokens properly in production

---

## Next Steps

1. ✅ Implement real JWT validation
2. ✅ Add proper error handling
3. ✅ Implement token refresh mechanism
4. ✅ Add role-based authorization
5. ✅ Set up logging and monitoring
6. ✅ Write comprehensive tests
7. ✅ Configure HTTPS in production

---

## License

This is a demonstration project. Use at your own risk in production environments.

## Contributing

Feel free to submit issues and enhancement requests!