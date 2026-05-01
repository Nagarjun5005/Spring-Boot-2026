package com.interceptorLearning.interceptor.entity.custom;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Student {

    int id;
    String firstName;
    String lastName;
}
