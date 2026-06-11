package com.lostres.ms_documental_dms;

import com.lostres.ms_documental_dms.config.DmsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(DmsProperties.class)
public class MsDocumentalDmsApplication {

	@PostConstruct
	void started() {
		// Ajustar la zona horaria de la JVM a la local del usuario (UTC-4)
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-4"));
	}

	public static void main(String[] args) {
		SpringApplication.run(MsDocumentalDmsApplication.class, args);
	}

}
