SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema llminsights
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `llminsights` DEFAULT CHARACTER SET utf8mb4 ;
USE `llminsights` ;

-- -----------------------------------------------------
-- Table `llminsights`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `llminsights`.`users` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `github_user_id` INT(11) NULL DEFAULT NULL,
  `user_name` VARCHAR(255) NULL DEFAULT NULL,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `user_access_token` VARCHAR(255) NULL DEFAULT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `account_type` VARCHAR(255) NULL DEFAULT NULL,
  `avatar_url` VARCHAR(255) NULL DEFAULT NULL,
  `company` VARCHAR(255) NULL DEFAULT NULL,
  `location` VARCHAR(255) NULL DEFAULT NULL,
  `bio` VARCHAR(255) NULL DEFAULT NULL,
  `twitter_username` VARCHAR(255) NULL DEFAULT NULL,
  `public_repos` INT(11) NULL DEFAULT NULL,
  `followers` INT(11) NULL DEFAULT NULL,
  `private_owned_repos` INT(11) NULL DEFAULT NULL,
  `created_at` DATETIME NULL DEFAULT NULL,
  `last_visited_on` DATETIME NULL DEFAULT NULL,
  `collaborators` INT(11) NULL DEFAULT NULL,
  `visible` CHAR(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 9
DEFAULT CHARACTER SET = utf8mb4;


-- -----------------------------------------------------
-- Table `llminsights`.`user_repos`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `llminsights`.`user_repos` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `github_repo_id` INT(11) NOT NULL,
  `user_id` INT(11) NULL DEFAULT NULL,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `private` TINYINT(1) NULL DEFAULT NULL,
  `fork` TINYINT(1) NULL DEFAULT NULL,
  `size` INT(11) NULL DEFAULT NULL,
  `has_issues` TINYINT(1) NULL DEFAULT NULL,
  `has_projects` TINYINT(1) NULL DEFAULT NULL,
  `has_downloads` TINYINT(1) NULL DEFAULT NULL,
  `has_wiki` TINYINT(1) NULL DEFAULT NULL,
  `forks_count` INT(11) NULL DEFAULT NULL,
  `forks` INT(11) NULL DEFAULT NULL,
  `open_issues` INT(11) NULL DEFAULT NULL,
  `open_issues_count` INT(11) NULL DEFAULT NULL,
  `default_branch` VARCHAR(255) NULL DEFAULT NULL,
  `language` VARCHAR(255) NULL DEFAULT NULL,
  `repo_created_at` DATETIME NULL DEFAULT NULL,
  `repo_updated_at` DATETIME NULL DEFAULT NULL,
  `created_at` DATETIME NULL DEFAULT NULL,
  `updated_at` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `user_repos_ibfk_1`
    FOREIGN KEY (`user_id`)
    REFERENCES `CSCI5308_12_DEVINT`.`users` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 65
DEFAULT CHARACTER SET = utf8mb4;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;