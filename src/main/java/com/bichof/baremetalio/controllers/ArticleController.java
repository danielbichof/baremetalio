package com.bichof.baremetalio.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.bichof.baremetalio.controllers.storage.ArticleStorageProperties;
import com.bichof.baremetalio.dtos.ArticleRecordDto;
import com.bichof.baremetalio.model.ArticlesModel;
import com.bichof.baremetalio.repositories.ArticleRepository;

import jakarta.validation.Valid;

@RestController
public class ArticleController {
	@Autowired
	ArticleRepository articleRepository;
	private final Path fileStorageLocation;

		public ArticleController(ArticleStorageProperties fileStorageProperties) {
		this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
				.toAbsolutePath().normalize();
	}

	@GetMapping("/articles")
	public ResponseEntity<List<ArticlesModel>> getAllArticles() {
		List<ArticlesModel> articlesList = articleRepository.findAll();
		if (!articlesList.isEmpty()) {
			for (ArticlesModel article : articlesList) {
				UUID id = article.getIdArticle();
				article.add(linkTo(methodOn(ArticleController.class).getOneArticle(id)).withSelfRel());
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(articlesList);
	}

	@GetMapping("/articles/{id}")
	public ResponseEntity<Object> getOneArticle(@PathVariable(value = "id") UUID id) {
		Optional<ArticlesModel> article = articleRepository.findById(id);
		if (article.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Article not found");
		}
		article.get().add(linkTo(methodOn(ArticleController.class).getAllArticles()).withRel("Articles list"));
		return ResponseEntity.status(HttpStatus.OK).body(
				article.get());
	}

	@PostMapping("/articles")
	public ResponseEntity<ArticlesModel> saveArticle(@RequestBody @Valid ArticleRecordDto postRecordDto) {
		ArticlesModel postModel = new ArticlesModel();
		BeanUtils.copyProperties(postRecordDto, postModel);
		return ResponseEntity.status(HttpStatus.CREATED).body(articleRepository.save(postModel));
	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestBody @Valid ArticleRecordDto postRecordDto, @RequestParam("file") MultipartFile file) {
		
		saveArticle(postRecordDto);
		
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());

		try {
			System.out.println(fileName);
			Path targetLocation = fileStorageLocation.resolve(fileName);
			System.out.println(targetLocation);
			file.transferTo(targetLocation);

			String fileDownload = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/files/download/")
					.path(fileName).toUriString();
			return ResponseEntity.status(HttpStatus.OK).body("Upload completed! Download link: " + fileDownload);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
		}
	}

	@PutMapping("/articles/{id}")
	public ResponseEntity<Object> updateArticle(@PathVariable UUID id,
			@RequestBody @Valid ArticleRecordDto articleRecordDto) {
		Optional<ArticlesModel> article = articleRepository.findById(id);
		if (article.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Article not found");
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
