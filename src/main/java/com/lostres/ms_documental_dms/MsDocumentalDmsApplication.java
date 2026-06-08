package com.lostres.ms_documental_dms;

import com.lostres.ms_documental_dms.config.DmsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(DmsProperties.class)
public class MsDocumentalDmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsDocumentalDmsApplication.class, args);
	}

}
