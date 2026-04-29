package com.interceptorLearning.interceptor.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
@Component
public class RequestInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                try{
                    System.out.println("1-prehandle : before sending request to the controller");
                    System.out.println("Method Type : "+request.getMethod());
                    System.out.println("Request Url : "+request.getRequestURI());
                }catch (Exception e){
                    e.printStackTrace();
                    return false;
                }
                return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,  ModelAndView modelAndView) throws Exception {
        try{
            System.out.println("2-postHandle(): After the controller serves the request (but before returning back response to the client)");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,  Exception ex) throws Exception {
        try{
            System.out.println("3- afterCompletion() : After the request and response is completed");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
