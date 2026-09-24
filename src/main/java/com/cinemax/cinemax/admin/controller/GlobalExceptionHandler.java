package com.cinemax.cinemax.admin.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(HttpServletRequest request, Exception ex) {
        try (FileWriter fw = new FileWriter("app-error.log", true)) {
            fw.write("Request: " + request.getRequestURL() + "\n");
            ex.printStackTrace(new PrintWriter(fw));
            fw.write("\n==========================================\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException(ex);
    }
}
