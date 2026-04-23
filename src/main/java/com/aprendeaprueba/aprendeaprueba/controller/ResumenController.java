package com.aprendeaprueba.aprendeaprueba.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aprendeaprueba.aprendeaprueba.model.Resumen;
import com.aprendeaprueba.aprendeaprueba.service.IAService;

@RestController
@RequestMapping("/api/resumenes")
public class ResumenController {

	@Autowired
    private IAService iaService;

    // Endpoint para generar el resumen con la IA
    @PostMapping("/generar")
    public Map<String, String> generar(@RequestBody Map<String, String> payload) {
        String contenido = payload.get("contenido");
        String resultado = iaService.generarResumenTexto(contenido);
        return Map.of("resumen", resultado);
    }
}
