package com.bichof.baremetalio.controllers.configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
public class ArticleStorageProperties {

	private final Logger logger = LoggerFactory.getLogger(ArticleStorageProperties.class);

	@Setter
	private String storageDir;

	private String tmpStorageArticles;

	private final String defaultStorageArticles = "articles";

	private void getAllArticles(Path tmpDir) {
		try {
			Path path = Paths.get(getClass().getClassLoader().getResource("articles").toURI());
			List<Path> files = Files.walk(path, 1)
					.filter(Files::isRegularFile)
					.map(Path::normalize)
					.collect(Collectors.toList());

			for (Path file : files) {
				Path destinationPath = tmpDir.resolve(file.getFileName().toString());
				Path destinationFile = Files.copy(file, destinationPath, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Arquivo copiado: " + destinationFile.toString());
			}

		} catch (URISyntaxException e) {
			logger.info("Erro ao tentar encontrar artigos! " + e.getMessage());
		} catch (IOException e) {
			logger.info("Erro ao mover arquivos! ");
			e.printStackTrace();
		}
	}

	@PostConstruct
	public void init() throws IOException {
		if (!StringUtils.hasLength(storageDir)) {
			storageDir = defaultStorageArticles;
		}

		Path tempDir = Files.createTempDirectory(Paths.get(storageDir).normalize().toString());
		logger.info("Diretório tmp: " + tempDir.toString());
		if (!Files.exists(tempDir)) {
			try {
				Files.createDirectories(tempDir);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		tmpStorageArticles = tempDir.toAbsolutePath().toString();

		getAllArticles(tempDir);
	}
}
