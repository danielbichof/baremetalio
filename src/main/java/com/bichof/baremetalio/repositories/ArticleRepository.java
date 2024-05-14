package com.bichof.baremetalio.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bichof.baremetalio.model.ArticlesModel;

@Repository
public interface ArticleRepository extends JpaRepository<ArticlesModel, UUID> {

}
