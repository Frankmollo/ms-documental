package com.lostres.ms_documental_dms.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Hora de negocio (Bolivia por defecto). Evita UTC del contenedor Docker. */
public final class AppClock {

    private static final ZoneId ZONE = ZoneId.of(
            System.getenv().getOrDefault("APP_TIMEZONE", "America/La_Paz")
    );

    private AppClock() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static ZoneId zone() {
        return ZONE;
    }
}
