package com.bichof.baremetalio.app.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bichof.baremetalio.utils.ArticleParser;

@Configuration
public class BarametalioConfig {
	@Bean
	public ArticleParser articleParser() {
		return new ArticleParser();
	}
}
