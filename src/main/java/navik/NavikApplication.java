package navik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NavikApplication {

	public static void main(String[] args) {
		SpringApplication.run(NavikApplication.class, args);
	}

}
