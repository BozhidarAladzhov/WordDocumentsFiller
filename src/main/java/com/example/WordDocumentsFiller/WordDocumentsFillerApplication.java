package com.example.WordDocumentsFiller;


import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WordDocumentsFillerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WordDocumentsFillerApplication.class, args);
	}



}
