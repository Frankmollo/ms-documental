package com.lostres.ms_documental_dms.exception;

public class InvalidUserException extends RuntimeException {

    public InvalidUserException() {
        super("Header X-User-Id es obligatorio y no puede ser 'anonymous'");
    }
}
