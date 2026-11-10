package com.tecsup.clima.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tecsup.clima.service.DatoInvalidoException;
import com.tecsup.clima.service.RecursoNoEncontradoException;

/**
 * Traduce los errores de validacion y de negocio a respuestas JSON
 * {@code {status, error, mensaje}} con el codigo HTTP adecuado.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException error) {
        return construir(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> datoInvalido(DatoInvalidoException error) {
        return construir(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    /** Errores de @Valid, p. ej. "latitud: debe ser menor que o igual a 90.0; ...". */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException error) {
        String mensaje = error.getBindingResult().getFieldErrors().stream()
            .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException error) {
        return construir(HttpStatus.BAD_REQUEST, "Cuerpo ausente o con formato JSON invalido");
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase());
        cuerpo.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
