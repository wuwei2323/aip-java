package cn.org.aopa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AopaDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(AopaDbApplication.class, args).close();
	}

}
