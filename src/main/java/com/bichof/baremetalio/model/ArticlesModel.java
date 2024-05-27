package com.bichof.baremetalio.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_ARTICLES")
public class ArticlesModel extends RepresentationModel<ArticlesModel> implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID idArticle;
	private String title;
	private Date date;
	private String author;
	private String description;
	private String link;

	public UUID getIdArticle() {
		return this.idArticle;
	}

	public String getTitle() {
		return this.title;
	}

	public Date getDate() {
		return this.date;
	}

	public String getAuthor() {
		return this.author;
	}

	public String getDescription() {
		return this.description;
	}

	public String getLink() {
		return this.link;
	}

	public void setIdArticle(UUID id) {
		this.idArticle = id;
	}

	public void setTitle(String name) {
		this.title = name;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLink(String link) {
		this.link = link;
	}
}
