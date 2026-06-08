package com.lostres.ms_documental_dms.exception;

public class FileSizeExceededException extends RuntimeException {

    public FileSizeExceededException(long sizeBytes, long maxBytes) {
        super("Tamaño de archivo (" + sizeBytes + " bytes) excede el máximo permitido (" + maxBytes + " bytes)");
    }
}
