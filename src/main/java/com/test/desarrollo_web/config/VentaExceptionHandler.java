package com.test.desarrollo_web.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.test.desarrollo_web.controller")
public class VentaExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> manejarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String nombre = ex.getName() != null ? ex.getName() : "parámetro";
        String valor = ex.getValue() != null ? ex.getValue().toString() : "";
        String mensaje = "Valor inválido para " + nombre + ": '" + valor + "'. Verifique los datos enviados.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("success", false, "message", mensaje));
    }
}
