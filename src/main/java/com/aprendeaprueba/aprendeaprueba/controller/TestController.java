package com.aprendeaprueba.aprendeaprueba.controller;

import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.aprendeaprueba.aprendeaprueba.model.Test;
import com.aprendeaprueba.aprendeaprueba.service.IAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
        String contenido = String.valueOf(request.get("contenido"));
        String userId = String.valueOf(request.get("userId"));
        String titulo = String.valueOf(request.get("titulo"));
        String categoria = String.valueOf(request.get("categoria"));

        int cantidadPreguntas = 10;
        try {
            cantidadPreguntas = Integer.parseInt(String.valueOf(request.get("cantidadPreguntas")));
            if (cantidadPreguntas < 1) cantidadPreguntas = 1;
            if (cantidadPreguntas > 30) cantidadPreguntas = 30;
        } catch (Exception ignored) {}

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
        nuevoTest.setTitulo("Test de " + titulo);
        nuevoTest.setCategoria(categoria);
        nuevoTest.setPreguntas(preguntas);
        return nuevoTest;
    }

}