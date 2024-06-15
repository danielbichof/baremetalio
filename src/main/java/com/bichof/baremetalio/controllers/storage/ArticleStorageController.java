package com.bichof.baremetalio.controllers.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
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
import jakarta.validation.Valid;

@Controller
@RequestMapping("/api")
public class ArticleStorageController {
	private static final Logger logger = LoggerFactory.getLogger(ArticleStorageController.class);

	@Autowired
	private ResourceLoader resourceLoader;
	@Autowired
	private ArticleUtils articleParser;
	@Autowired
	ArticleRepository articleRepository;
	@Autowired
	ObjectMapper objectMapper;

	private final Path tmpStorageLocation;

	public ArticleStorageController(ArticleStorageProperties fileStorageProperties) {
		this.tmpStorageLocation = Paths.get(fileStorageProperties.getTmpStorageArticles())
				.toAbsolutePath().normalize();
	}

	@GetMapping("/download/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {

		Path filePath = tmpStorageLocation.resolve(fileName).normalize();
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
		List<ArticleWithLinkDto> articles = Files.list(tmpStorageLocation)
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

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadArticle(
			@RequestParam("articleRecordDto") String articleJson,
			@RequestPart("file") MultipartFile file) {

		String fileName = StringUtils.cleanPath(file.getOriginalFilename());

		ArticleRecordDto articleRecordDto = null;
		try {
			articleRecordDto = objectMapper.readValue(articleJson, ArticleRecordDto.class);
		} catch (JsonProcessingException e) {
			logger.error("Objeto articleRecordDto recebido invalido: " + articleJson + "	Erro:" + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					"Objeto articleRecordDto recebido invalido: " + articleJson + "	Erro:" + e.getMessage());
		}

		try {
			// Print the received data for debugging
			System.out.println("Title: " + articleRecordDto.title());
			System.out.println("Tags: " + articleRecordDto.tags());
			System.out.println("File: " + fileName);

			articleParser.checkDirExists(tmpStorageLocation);
			Path targetFile = tmpStorageLocation.resolve(fileName);
			logger.info("Arquivo gravado: " + targetFile.toString() + " no diretório " + tmpStorageLocation.toString());
			file.transferTo(targetFile);

			Path targetCompressedFile = Paths.get(targetFile.toString() + ".gz");
			ArticleUtils.compressGzip(targetFile, targetCompressedFile);
			byte[] compressedFile = Files.readAllBytes(targetCompressedFile);

			String fileDownload = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/download/")
					.path(fileName).toUriString();
			return ResponseEntity.status(HttpStatus.OK).body("Upload completed! Download link: " + fileDownload);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
		}
	}
}
