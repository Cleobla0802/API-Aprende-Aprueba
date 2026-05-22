package com.aprendeaprueba.aprendeaprueba.controller;

import com.aprendeaprueba.aprendeaprueba.service.IAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/apuntes")
public class ApunteController {

    private static final long MAX_ARCHIVO_BYTES = 6L * 1024L * 1024L;

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

    @PostMapping(
        value = "/digitalizar-archivo",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> digitalizarArchivo(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibio ningun archivo");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser una imagen");
        }

        if (archivo.getSize() > MAX_ARCHIVO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamano maximo permitido");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(archivo.getBytes());
            String dataUrl = "data:" + contentType + ";base64," + base64;
            String textoIA = serviceIA.digitalizar(dataUrl);
            return ResponseEntity.ok(textoIA);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen", e);
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}
