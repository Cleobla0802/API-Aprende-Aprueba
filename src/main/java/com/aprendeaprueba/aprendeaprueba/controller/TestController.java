package com.aprendeaprueba.aprendeaprueba.controller;

import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.aprendeaprueba.aprendeaprueba.model.Test;
import com.aprendeaprueba.aprendeaprueba.service.IAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tests")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private IAService iaService;

    @PostMapping("/generar")
    public Test crearTest(@RequestBody Map<String, String> request) {
        String contenido = request.get("contenido");
        String userId = request.get("userId");
        String titulo = request.get("titulo");
        String categoria = request.get("categoria");

        // 1. Generar preguntas con IA
        List<Pregunta> preguntas = iaService.generarPreguntasIA(contenido);

        // 2. Crear objeto Test
        Test nuevoTest = new Test();
        nuevoTest.setUserId(userId);
        nuevoTest.setTitulo("Test de " + titulo);
        nuevoTest.setCategoria(categoria);
        nuevoTest.setPreguntas(preguntas);

        // 3. Guardar en Firebase
        iaService.guardarTestFirebase(nuevoTest);

        return nuevoTest;
    }
}