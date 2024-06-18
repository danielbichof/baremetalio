package com.bichof.baremetalio.dtos;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleSummaryDTO extends RepresentationModel<ArticleSummaryDTO> {
	private UUID id;
	private String title;
	private Date date;
	private String author;
	private String description;
	private List<String> tags;
	private String fileGzipUrl;
	private byte[] md5;
}
