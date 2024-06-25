package com.bichof.baremetalio.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "TB_ARTICLES")
public class ArticlesModel extends RepresentationModel<ArticlesModel> implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;
	private String title;
	private Date date;
	private String author;
	private String description;
	private List<String> tags;
	@Lob
	@Column(columnDefinition = "MEDIUMBLOB")
	private byte[] fileGzip;
	@Lob
	@Column(columnDefinition = "TINYBLOB")
	private byte[] md5;

	@Transient
	private String fileGzipUrl;
}
