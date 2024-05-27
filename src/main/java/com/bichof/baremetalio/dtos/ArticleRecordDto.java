package com.bichof.baremetalio.dtos;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleRecordDto(@NotBlank String title,
	@NotNull Date date, 
	@NotBlank String author,
	@NotBlank String description, 
	@NotBlank String link) {

}
