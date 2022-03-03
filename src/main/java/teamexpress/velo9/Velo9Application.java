package teamexpress.velo9;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Velo9Application {

	public static void main(String[] args) {
		SpringApplication.run(Velo9Application.class, args);
	}

}
