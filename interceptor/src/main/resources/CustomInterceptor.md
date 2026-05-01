# 🚀 Spring Boot Custom Interceptor Example

This project demonstrates how to implement a **custom interceptor** in Spring Boot to handle HTTP requests at different stages of the request lifecycle.

---

## 📌 Overview

A **Spring Boot Interceptor** allows you to:

* Intercept HTTP requests before they reach the controller
* Process logic after controller execution
* Perform actions after the response is completed

This project logs request details using a custom interceptor.

---

## 🧩 Project Structure

```
com.interceptorLearning.interceptor
│
├── config
│   ├── RequestInterceptor.java
│   └── RequestInterceptorConfig.java
│
├── controller
│   └── StudentController.java
│
├── entity
│   └── Student.java
```

---

## ⚙️ Interceptor Implementation

### 1. RequestInterceptor

Implements `HandlerInterceptor` and overrides lifecycle methods:

* `preHandle()` → Before controller execution
* `postHandle()` → After controller execution
* `afterCompletion()` → After response completion

### 🔍 Code Behavior

```java
System.out.println("1-prehandle : before sending request to the controller");
System.out.println("Method Type : " + request.getMethod());
System.out.println("Request Url : " + request.getRequestURI());
```

---

## 🔄 Interceptor Lifecycle

```
Client Request
     ↓
preHandle()        → Before controller
     ↓
Controller Logic
     ↓
postHandle()       → After controller
     ↓
afterCompletion()  → After response
     ↓
Client Response
```

---

## ⚙️ Interceptor Configuration

The interceptor is registered using `WebMvcConfigurer`.

```java
registry.addInterceptor(new RequestInterceptor());
```

👉 This applies the interceptor to **all endpoints**.

---

## 🎯 Sample API

### Endpoint

```
GET /students
```

### Response

```json
[
  { "id": 1, "firstName": "Adwitiya", "lastName": "Mourya" },
  { "id": 2, "firstName": "David", "lastName": "Goggins" },
  { "id": 3, "firstName": "Andrew", "lastName": "Huberman" }
]
```

---

## 🧪 Console Output

When calling `/students`:

```
1-prehandle : before sending request to the controller
Method Type : GET
Request Url : /students

2-postHandle(): After the controller serves the request

3- afterCompletion() : After the request and response is completed
```

---

## 🧠 Key Concepts

| Method          | Purpose                                   |
| --------------- | ----------------------------------------- |
| preHandle       | Runs before controller execution          |
| postHandle      | Runs after controller but before response |
| afterCompletion | Runs after complete request lifecycle     |

---

## ⚠️ Important Notes

* Returning `false` in `preHandle()` stops the request.
* Interceptors must be **registered**, otherwise they won’t execute.
* Use logging frameworks (e.g., SLF4J) instead of `System.out.println` in production.

---

## 🚀 Use Cases

Common real-world uses of interceptors:

* Logging & monitoring
* Authentication checks
* Request validation
* Correlation ID tracking (microservices)
* Audit logging

---

## 🔧 Future Improvements

* Add **SLF4J logging**
* Implement **JWT validation**
* Add **custom annotations for selective interception**
* Integrate with **Spring Security**

---

## 🏁 How to Run

1. Clone the repository
2. Build the project:

   ```
   mvn clean install
   ```
3. Run the application:

   ```
   mvn spring-boot:run
   ```
4. Access:

   ```
   http://localhost:8080/students
   ```

---

## 💡 Summary

This project demonstrates how to:

* Create a custom interceptor
* Register it in Spring Boot
* Track request lifecycle effectively

---
