package com.bichof.baremetalio.controllers.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.bichof.baremetalio.controllers.configuration.ArticleStorageProperties;
import com.bichof.baremetalio.dtos.ArticleRecordDto;
import com.bichof.baremetalio.dtos.ArticleWithLinkDto;
import com.bichof.baremetalio.model.ArticlesModel;
import com.bichof.baremetalio.repositories.ArticleRepository;
import com.bichof.baremetalio.utils.ArticleUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api")
public class ArticleStorageController {
	private static final Logger logger = LoggerFactory.getLogger(ArticleStorageController.class);

	@Autowired
	private ResourceLoader resourceLoader;
	private final Path uploadStorageLocation;
	private final Path readStorageLocation;
	private ArticleParser articleParser;

	public ArticleStorageController(ArticleStorageProperties fileStorageProperties, ArticleParser articleParser) {
		this.uploadStorageLocation = Paths.get(fileStorageProperties.getStorageDir())
				.toAbsolutePath().normalize();
		this.readStorageLocation = Paths.get(fileStorageProperties.getTmpStorageArticles())
				.toAbsolutePath().normalize();
		this.articleParser = articleParser;
	}

	@GetMapping("/download/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {

		Path filePath = readStorageLocation.resolve(fileName).normalize();
		logger.info("Requisitado download do arquivo: " + filePath.toString());

		try {
			Resource resource = new UrlResource(filePath.toUri());

			if (!Files.exists(filePath) || !resource.exists()) {
				logger.error("Arquivo não encontrado: " + filePath.toString());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}

			String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

			if (contentType == null) {
				contentType = "text/plain";
			}

			String headerProperties = String.format("attachment; filename=\"%s\"", resource.getFilename());
			return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, headerProperties)
					.body(resource);
		} catch (MalformedURLException e) {
			logger.error(
					"Erro, url do arquivo invalida: "
							+ filePath.toString()
							+ "\n"
							+ "threw exception: "
							+ e.getMessage(),
					e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (IOException e) {
			logger.error(
					"Falha ao procurar arquivo: " + filePath.toString() + " " + "threw exception: " + e.getMessage(),
					e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/list")
	public ResponseEntity<List<ArticleWithLinkDto>> listArticles() throws IOException, ParseException {
		List<ArticleWithLinkDto> articles = Files.list(readStorageLocation)
				.filter(Files::isRegularFile)
				.map(path -> {
					try {
						Resource resource = resourceLoader.getResource("file:" + path.toString());
						ArticleRecordDto articleRecord = articleParser.parseHeader(resource);
						String downloadLink = ServletUriComponentsBuilder.fromCurrentContextPath()
								.path("/api/download/")
								.path(path.getFileName().toString())
								.toUriString();
						return new ArticleWithLinkDto(articleRecord, downloadLink);
					} catch (Exception e) {
						throw new RuntimeException("Falha ao proecessar: " + path.toString(), e);
					}
				})
				.collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.OK).body(articles);
	}

	@GetMapping("/articles/{fileName:.+}")

	// @PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestBody @RequestParam("file") MultipartFile file) {

		String fileName = StringUtils.cleanPath(file.getOriginalFilename());

		try {
			Path targetLocation = uploadStorageLocation.resolve(fileName);
			System.out.println(targetLocation);
			file.transferTo(targetLocation);

			String fileDownload = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/download/")
					.path(fileName).toUriString();
			return ResponseEntity.status(HttpStatus.OK).body("Upload completed! Download link: " + fileDownload);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
		}
	}
}
