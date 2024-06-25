package com.bichof.baremetalio.controllers.storage;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HexFormat;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.bichof.baremetalio.controllers.ArticlesResponseWithLink;
import com.bichof.baremetalio.controllers.configuration.ArticleStorageProperties;
import com.bichof.baremetalio.dtos.ArticleRecordDto;
import com.bichof.baremetalio.dtos.ArticleSummaryDTO;
import com.bichof.baremetalio.model.ArticlesModel;
import com.bichof.baremetalio.repositories.ArticleRepository;
import com.bichof.baremetalio.utils.ArticleUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
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

	@GetMapping("/articles")
	public ResponseEntity<List<ArticleSummaryDTO>> getAllArticles() throws IOException {
		List<ArticlesModel> articlesList = articleRepository.findAll();
		List<ArticleSummaryDTO> articleSummaryDTOS = articlesList.stream().map(article -> {
			ArticleSummaryDTO dto = new ArticleSummaryDTO();
			BeanUtils.copyProperties(article, dto);
			dto.setFileGzipUrl(linkTo(methodOn(ArticleStorageController.class).getFileGzip(
					article.getId())).toUri().toString());
			try {
				dto.add(linkTo(
						methodOn(ArticleStorageController.class).getOneArticle(article.getId()))
						.withSelfRel());
			} catch (IOException e) {
				logger.error("Erro ao criar adicionar link do arquivo" + e.getMessage(), e);
			}
			return dto;
		}).collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.OK).body(articleSummaryDTOS);
	}

	@GetMapping("/articles/{id}")
	public ResponseEntity<ArticleSummaryDTO> getOneArticle(@PathVariable(value = "id") UUID id) throws IOException {
		Optional<ArticlesModel> articleList = articleRepository.findById(id);
		if (articleList.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_ENCODING, "application/json");
		
		ArticlesModel article = articleList.get();
		ArticleSummaryDTO dto = new ArticleSummaryDTO();
		BeanUtils.copyProperties(article, dto);

		dto.setFileGzipUrl(linkTo(methodOn(ArticleStorageController.class).getFileGzip(id)).toUri().toString());
		dto.add(linkTo(methodOn(ArticleStorageController.class).getAllArticles()).withRel("Articles list"));

		return ResponseEntity.status(HttpStatus.OK)
				.headers(headers)
				.body(dto);
	}

	@GetMapping("/articles/{id}/file")
	public ResponseEntity<byte[]> getFileGzip(@Valid @PathVariable(value = "id") UUID id) {
		Optional<ArticlesModel> article = articleRepository.findById(id);
		if (article.isPresent()) {
			byte[] fileGzip = article.get().getFileGzip();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.set(HttpHeaders.CONTENT_ENCODING, "gzip");
			headers.setContentDispositionFormData("attachment", "file.md");

			return ResponseEntity.status(HttpStatus.OK).headers(headers).body(fileGzip);

		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	// @GetMapping("/list")
	public ResponseEntity<List<ArticlesResponseWithLink>> listArticles() throws IOException, ParseException {
		List<ArticlesResponseWithLink> articles = Files.list(tmpStorageLocation)
				.filter(Files::isRegularFile)
				.map(path -> {
					try {
						Resource resource = resourceLoader.getResource("file:" + path.toString());
						ArticleRecordDto articleRecord = articleParser.parseHeader(resource);
						String downloadLink = ServletUriComponentsBuilder.fromCurrentContextPath()
								.path("/api/download/")
								.path(path.getFileName().toString())
								.toUriString();
						return new ArticlesResponseWithLink(articleRecord, downloadLink);
					} catch (Exception e) {
						throw new RuntimeException("Falha ao proecessar: " + path.toString(), e);
					}
				})
				.collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.OK).body(articles);
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ArticleSummaryDTO> uploadArticle(
			@Valid @RequestParam("articleRecordDto") String articleJson,
			@Valid @RequestPart("file") MultipartFile file) {

		String fileName = StringUtils.cleanPath(file.getOriginalFilename());

		ArticleRecordDto articleRecordDto = null;
		try {
			articleRecordDto = objectMapper.readValue(articleJson, ArticleRecordDto.class);
		} catch (JsonProcessingException e) {
			String message = "Objeto articleRecordDto recebido invalido: " + articleJson + "	Erro:" + e.getMessage();
			logger.error(message, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		var articleModel = new ArticlesModel();
		try {

			BeanUtils.copyProperties(articleRecordDto, articleModel);

			logger.info("Artigo recebido: " + articleRecordDto.title() + ", arquivo: " + fileName);
			byte[] compressedFile = getZipedFile(file, fileName);

			if (compressedFile != null && articleRecordDto.getMd5() != null) {
				articleModel.setFileGzip(compressedFile);
				articleModel.setMd5(HexFormat.of().parseHex(articleRecordDto.getMd5()));
				articleRepository.save(articleModel);
				ArticleSummaryDTO dto = new ArticleSummaryDTO();
				BeanUtils.copyProperties(articleModel, dto);
				dto.setFileGzipUrl(linkTo(methodOn(ArticleStorageController.class).getFileGzip(
						articleModel.getId())).toUri().toString());
				try {
					dto.add(linkTo(
							methodOn(ArticleStorageController.class).getOneArticle(articleModel.getId()))
							.withSelfRel());
				} catch (IOException e) {
					logger.error("Erro ao criar adicionar link do arquivo" + e.getMessage(), e);
				}
				return ResponseEntity.ok().body(dto);
			}

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ArticlesModel> updateArticle(
			@Valid @RequestParam("articleRecordDto") String articleJson,
			@Valid @RequestPart("file") MultipartFile file) {

		String fileName = StringUtils.cleanPath(file.getOriginalFilename());

		ArticleRecordDto articleRecordDto = null;
		try {
			articleRecordDto = objectMapper.readValue(articleJson, ArticleRecordDto.class);
		} catch (JsonProcessingException e) {
			String message = "Objeto articleRecordDto recebido invalido: " + articleJson + "	Erro:" + e.getMessage();
			logger.error(message, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		var articleModel = new ArticlesModel();

		try {

			BeanUtils.copyProperties(articleRecordDto, articleModel);

			byte[] compressedFile = getZipedFile(file, fileName);

			if (compressedFile != null && articleRecordDto.getMd5() != null) {
				articleModel.setFileGzip(compressedFile);
				articleModel.setMd5(HexFormat.of().parseHex(articleRecordDto.getMd5()));
				return ResponseEntity.ok().body(articleRepository.save(articleModel));
			}

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private byte[] getZipedFile(MultipartFile file, String fileName) throws IOException {
		articleParser.checkDirExists(tmpStorageLocation);

		Path targetFile = tmpStorageLocation.resolve(fileName);
		file.transferTo(targetFile);
		
		logger.info("Arquivo gravado: " + targetFile.toString() + " no diretório " + tmpStorageLocation.toString());
		Path targetCompressedFile = Paths.get(targetFile.toString() + ".gz");
		ArticleUtils.compressGzip(targetFile, targetCompressedFile);
		return Files.readAllBytes(targetCompressedFile);
	}

	@PutMapping("/articles/{id}")
	public ResponseEntity<ArticlesModel> updateArticle(@PathVariable UUID id,
			@RequestBody @Valid ArticleRecordDto articleRecordDto) {
		Optional<ArticlesModel> article = articleRepository.findById(id);
		if (article.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		var articleModel = article.get();
		BeanUtils.copyProperties(articleRecordDto, articleModel);
		return ResponseEntity.status(HttpStatus.OK).body(articleRepository.save(articleModel));
	}

	@DeleteMapping("/articles/{id}")
	public ResponseEntity<Object> deleteArticle(@PathVariable(value = "id") UUID id) {
		Optional<ArticlesModel> article = articleRepository.findById(id);
		if (article.isEmpty())
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Article not found");
		articleRepository.delete(article.get());
		return ResponseEntity.status(HttpStatus.OK).body("Article deleted successfully!");
	}

}
