package com.lostres.ms_documental_dms.exception;

public class S3ObjectNotFoundException extends RuntimeException {

    public S3ObjectNotFoundException(String s3Key) {
        super("El archivo no existe en S3 para la clave: " + s3Key);
    }
}
