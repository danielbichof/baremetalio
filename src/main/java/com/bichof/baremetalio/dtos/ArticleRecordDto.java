package com.bichof.baremetalio.dtos;

import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleRecordDto(
		@NotBlank String title,
		@NotNull Date date,
		@NotBlank String author,
		@NotBlank String description,
		@NotNull List<String> tags,
		@NotBlank String md5) {
	
	public String getMd5(){
		return md5;
	}
}
