package com.aprendeaprueba.aprendeaprueba.controller;

import com.aprendeaprueba.aprendeaprueba.service.IAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/apuntes")
public class ApunteController {

    @Autowired
    private IAService serviceIA;

    @PostMapping("/digitalizar")
    public ResponseEntity<Map<String, String>> digitalizar(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        String textoIA = serviceIA.digitalizar(url);
        
        Map<String, String> response = new HashMap<>();
        response.put("textoIA", textoIA);
        
        return ResponseEntity.ok(response);
    }
}