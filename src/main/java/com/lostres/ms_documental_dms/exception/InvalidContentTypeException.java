package com.lostres.ms_documental_dms.exception;

public class InvalidContentTypeException extends RuntimeException {

    public InvalidContentTypeException(String contentType) {
        super("Tipo de contenido no permitido: " + contentType);
    }
}
