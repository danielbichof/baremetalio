package com.bichof.baremetalio.controllers.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api")
public class ArticleStorageController {
	private final Path fileStorageLocation;

	public ArticleStorageController(ArticleStorageProperties fileStorageProperties) {
		this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
				.toAbsolutePath().normalize();
	}

	@GetMapping("/download/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request)
			throws IOException {
		Path filePath = fileStorageLocation.resolve(fileName).normalize();

		try {
			Resource resource = new UrlResource(filePath.toUri());
			String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
			return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@GetMapping("/list")
	public ResponseEntity<List<String>> listArticles() throws IOException {
		List<String> fileNames = Files.list(fileStorageLocation)
			.map(Path::getFileName)
			.map(Path::toString)
			.collect(Collectors.toList());

			return ResponseEntity.status(HttpStatus.OK).body(fileNames);
	}

}
