package com.aprendeaprueba.aprendeaprueba.controller;

import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.aprendeaprueba.aprendeaprueba.model.Test;
import com.aprendeaprueba.aprendeaprueba.service.IAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    @Autowired
    private IAService iaService;

    @PostMapping("/generar")
    public Test crearTest(@RequestBody Map<String, Object> request) {
        String contenido = obtenerTexto(request, "contenido");
        String userId = obtenerTexto(request, "userId");
        String titulo = obtenerTexto(request, "titulo");
        String categoria = obtenerTexto(request, "categoria");
        int cantidadPreguntas = obtenerCantidadPreguntas(request);

        if (contenido.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido no puede estar vacio");
        }

        List<Pregunta> preguntas;
        try {
            preguntas = iaService.generarPreguntasIA(contenido, cantidadPreguntas);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudieron generar preguntas: " + e.getMessage()
            );
        }

        Test nuevoTest = new Test();
        nuevoTest.setUserId(userId);
        nuevoTest.setTitulo(titulo);
        nuevoTest.setCategoria(categoria);
        nuevoTest.setPreguntas(preguntas);
        return nuevoTest;
    }

    private String obtenerTexto(Map<String, Object> request, String clave) {
        Object valor = request.get(clave);
        if (valor == null) return "";

        String texto = String.valueOf(valor).trim();
        return "null".equalsIgnoreCase(texto) ? "" : texto;
    }

    private int obtenerCantidadPreguntas(Map<String, Object> request) {
        try {
            int cantidad = Integer.parseInt(String.valueOf(request.get("cantidadPreguntas")));
            return Math.max(1, Math.min(cantidad, 15));
        } catch (Exception ignored) {
            return 10;
        }
    }
}
