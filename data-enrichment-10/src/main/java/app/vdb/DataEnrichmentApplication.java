package app.vdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DataEnrichmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataEnrichmentApplication.class, args);
	}

}
