package com.loudent.library.aspect;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TimedSync {
  String metric();

  String[] tags() default {};
}
