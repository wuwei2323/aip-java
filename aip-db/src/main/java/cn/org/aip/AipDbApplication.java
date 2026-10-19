package cn.org.aip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AipDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(AipDbApplication.class, args).close();
	}

}
