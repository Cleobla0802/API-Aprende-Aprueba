package com.aprendeaprueba.aprendeaprueba.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aprendeaprueba.aprendeaprueba.service.IAService;

@RestController
@RequestMapping("/api/resumenes")
public class ResumenController {

    @Autowired
    private IAService iaService;

    @PostMapping("/generar")
    public Map<String, String> generar(@RequestBody Map<String, String> payload) {
        String contenido = payload.getOrDefault("contenido", "").trim();
        if (contenido.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido no puede estar vacio");
        }

        String resultado = iaService.generarResumenTexto(contenido);
        return Map.of("resumen", resultado);
    }
}
