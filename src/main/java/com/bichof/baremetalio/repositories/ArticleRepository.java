package com.bichof.baremetalio.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bichof.baremetalio.model.ArticlesModel;

public interface ArticleRepository  extends JpaRepository<ArticlesModel, UUID> {

}
