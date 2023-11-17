-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               8.0.27 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             11.3.0.6295
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for llminsights
DROP DATABASE IF EXISTS `llminsights`;
CREATE DATABASE IF NOT EXISTS `llminsights` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `llminsights`;

-- Dumping structure for table llminsights.repo_loc
DROP TABLE IF EXISTS `repo_loc`;
CREATE TABLE IF NOT EXISTS `repo_loc` (
  `id` int NOT NULL AUTO_INCREMENT,
  `repo_id` int NOT NULL DEFAULT '0',
  `language` varchar(255) DEFAULT NULL,
  `files` int DEFAULT NULL,
  `lines` int DEFAULT NULL,
  `blanks` int DEFAULT NULL,
  `comments` int DEFAULT NULL,
  `linesOfCode` int DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `repo_id` (`repo_id`),
  CONSTRAINT `FK_repo_loc_user_repos` FOREIGN KEY (`repo_id`) REFERENCES `user_repos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Data exporting was unselected.

-- Dumping structure for table llminsights.users
DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `github_user_id` int DEFAULT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `user_access_token` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `account_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `company` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `twitter_username` varchar(255) DEFAULT NULL,
  `public_repos` int DEFAULT NULL,
  `followers` int DEFAULT NULL,
  `private_owned_repos` int DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `last_visited_on` datetime DEFAULT NULL,
  `collaborators` int DEFAULT NULL,
  `visible` char(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Data exporting was unselected.

-- Dumping structure for table llminsights.user_repos
DROP TABLE IF EXISTS `user_repos`;
CREATE TABLE IF NOT EXISTS `user_repos` (
  `id` int NOT NULL AUTO_INCREMENT,
  `github_repo_id` int NOT NULL,
  `user_id` int DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  `private` tinyint(1) DEFAULT NULL,
  `fork` tinyint(1) DEFAULT NULL,
  `size` int DEFAULT NULL,
  `has_issues` tinyint(1) DEFAULT NULL,
  `has_projects` tinyint(1) DEFAULT NULL,
  `has_downloads` tinyint(1) DEFAULT NULL,
  `has_wiki` tinyint(1) DEFAULT NULL,
  `forks_count` int DEFAULT NULL,
  `forks` int DEFAULT NULL,
  `open_issues` int DEFAULT NULL,
  `open_issues_count` int DEFAULT NULL,
  `default_branch` varchar(255) DEFAULT NULL,
  `language` varchar(255) DEFAULT NULL,
  `repo_created_at` datetime DEFAULT NULL,
  `repo_updated_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_repos_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Data exporting was unselected.

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;