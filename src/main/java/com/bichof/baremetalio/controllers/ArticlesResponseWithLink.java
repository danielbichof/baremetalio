package com.bichof.baremetalio.controllers;

import com.bichof.baremetalio.dtos.ArticleRecordDto;

import lombok.Data;

@Data
public class ArticlesResponseWithLink {
	
	protected ArticleRecordDto articleRecordDto;

	protected String message;
	public ArticlesResponseWithLink(ArticleRecordDto articleRecordDto, String message){
		this.articleRecordDto = articleRecordDto;
		this.message = message;
	}
}
