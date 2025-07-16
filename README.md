# Course Search Application with Spring Boot and Elasticsearch

This project is a Spring Boot application demonstrating full-text search and autocomplete (suggestions) functionalities using Elasticsearch. It allows users to search for courses based on various criteria and get real-time suggestions as they type.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Setup and Running the Application](#setup-and-running-the-application)
  - [Start Elasticsearch (using Docker)](#start-elasticsearch-using-docker)
  - [Build and Run the Spring Boot Application](#build-and-run-the-spring-boot-application)
- [API Endpoints](#api-endpoints)
  - [Get All Courses (or Initial Search Results)](#get-all-courses-or-initial-search-results)
  - [Search Courses by Query and Filters](#search-courses-by-query-and-filters)
  - [Get Autocomplete Suggestions](#get-autocomplete-suggestions)
- [Project Structure Highlights](#project-structure-highlights)

## Features

* **Full-Text Search:** Search courses by keywords in titles and descriptions.
* **Filtered Search:** Filter courses by category, type, minimum/maximum age, and price range.
* **Autocomplete Suggestions:** Get real-time course title/keyword suggestions as you type using Elasticsearch's Completion Suggester.
* **Automatic Data Loading:** On application startup, it automatically deletes the existing Elasticsearch index (if any), creates a new one with custom mappings/settings, and bulk indexes 50 randomly generated sample courses using JavaFaker.

## Technologies Used

* **Java 21.0.4**
* **Spring Boot 3.2.5**
* **Elasticsearch 8.11.3**
* **Elasticsearch Java API Client** (co.elastic.clients)
* **Maven** (for dependency management and build)
* **Lombok** (for boilerplate code reduction)
* **JavaFaker 1.0.2** (for generating sample data)
* **Docker** (for running Elasticsearch)

## Prerequisites

Before running this application, ensure you have the following installed:

* **Java Development Kit (JDK) 21** or later.
* **Apache Maven 3.x** or later.
* **Docker Desktop** (or Docker Engine) - essential for running Elasticsearch.

## Setup and Running the Application

Follow these steps to get the application up and running:

### Start Elasticsearch (using Docker)

Navigate to the root directory of your project where your `docker-compose.yml` file is located (typically `course-search/`).

```bash
cd /path/to/your/project/course-search
docker-compose up -d