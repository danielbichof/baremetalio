#!/usr/bin/env bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run