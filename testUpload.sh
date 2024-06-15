#!/usr/bin/env bash


curl -X POST \
  -F 'articleRecordDto={"title":"Exemplo de Artigo","date":"2024-06-15","author":"Autor","description":"Descrição do artigo","tags":["tag1","tag2"],"md5":"dummy-md5-hash"};type=application/json' \
  -F 'file=@src/main/resources/articles/2024-06-05-Git-do-jeito-certo.md' \
  http://localhost:8080/api/upload