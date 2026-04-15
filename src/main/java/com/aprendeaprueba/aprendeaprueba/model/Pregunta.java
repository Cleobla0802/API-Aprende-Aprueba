package com.aprendeaprueba.aprendeaprueba.model;

import java.util.List;

public class Pregunta {
    private String enunciado;
    private List<String> opciones;
    private int respuestaCorrecta; // Índice de la opción correcta (0, 1, 2...)

    // Getters y Setters
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public List<String> getOpciones() { return opciones; }
    public void setOpciones(List<String> opciones) { this.opciones = opciones; }
    public int getRespuestaCorrecta() { return respuestaCorrecta; }
    public void setRespuestaCorrecta(int respuestaCorrecta) { this.respuestaCorrecta = respuestaCorrecta; }
}