package com.bichof.baremetalio.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.bichof.baremetalio.dtos.ArticleRecordDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

@Component
public class ArticleParser {
	private static final Logger logger = LoggerFactory.getLogger(ArticleParser.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ArticleRecordDto parseHeader(Resource resource) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder yamlContent = new StringBuilder();
			String line;
			boolean yamlStarted = false;

			// Read the YAML front matter
			while ((line = reader.readLine()) != null) {
				if (line.trim().equals("---")) {
					if (yamlStarted) {
						break;
					} else {
						yamlStarted = true;
					}
				} else if (yamlStarted) {
					yamlContent.append(line).append("\n");
				}
			}

			// Parse YAML content
			Yaml yaml = new Yaml();
			Map<String, Object> yamlMap = yaml.load(yamlContent.toString());

			// Convert YAML map to ArticleRecordDto
			String title = (String) yamlMap.get("title");
			Date date = (Date) yamlMap.get("date");
			String author = (String) yamlMap.get("author");
			String description = (String) yamlMap.get("description");
			List<String> tags = objectMapper.convertValue(
					yamlMap.get("tags"),
					TypeFactory.defaultInstance().constructCollectionType(List.class, String.class));

			return new ArticleRecordDto(title, date, author, description, tags);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse YAML header", e);
		}
	}

	private Date parseDate(String sourceDate) {
		SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return parser.parse(sourceDate);
		} catch (ParseException e) {
			logger.error("Não foi possível fazer parser da data, retornando data atual", e);
		}
		return new Date();
	}
}
