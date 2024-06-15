package com.bichof.baremetalio.app.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bichof.baremetalio.utils.ArticleUtils;

@Configuration
public class BarametalioConfig {
	@Bean
	public ArticleUtils articleUtils() {
		return new ArticleUtils();
	}
}
