package com.lostres.ms_documental_dms;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requiere LocalStack (S3 + DynamoDB) en ejecución para cargar el contexto de Spring.")
class MsDocumentalDmsApplicationTests {

	@Test
	void contextLoads() {
	}

}
