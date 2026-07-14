package com.exam_system.exam.domain;

/**
 * Estados posibles de una resolución desde que el estudiante la inicia
 * hasta que el profesor termina de corregirla.
 */
public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    GRADED
}
