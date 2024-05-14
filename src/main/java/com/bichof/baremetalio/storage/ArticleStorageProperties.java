package com.bichof.baremetalio.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

// Class that control the storage of articles in disks
@Configuration
@ConfigurationProperties(prefix="file")
public class ArticleStorageProperties {
	@Getter
	@Setter
	private String uploadDir;

}
