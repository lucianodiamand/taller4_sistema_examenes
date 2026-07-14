package com.exam_system.shared.api;

/**
 * Se usa cuando la petición es válida, pero el estado actual del examen
 * no permite completar la operación, por ejemplo si ya fue enviado.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
