package com.bichof.baremetalio.utils;

import org.springframework.core.io.Resource;

import com.bichof.baremetalio.dtos.ArticleRecordDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArticleResponse {
	ArticleRecordDto articleInformations;
	private Resource file;
}