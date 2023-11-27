
Table of Contents
=================

* [CodeInsighter](#codeinsighter)
* [Pre-requisites](#pre-requisites)
* [Local Database Setup](#local-database-setup)
* [Running the Application Locally](#running-the-application-locally)
* [Deployment Steps](#deployment-steps)
* [External Dependencies](#external-dependencies)
* [Resources Used for CodeInsighter](#resources-used-for-codeinsighter)
* [User Scenarios](#user-scenarios)
* [Contributors](#contributors)
* [Smell Analysis Summary](#smell-analysis-summary)
* [Member Contribution](#member-contribution)
* [Client Team Feedback](#client-team-feedback)


# CodeInsighter

"CodeInsighter" is a Java Spring Boot application engineered to provide comprehensive insights into software development practices. By leveraging its suite of functionalities, it actively identifies and addresses common coding errors, facilitates collaborative analysis, enhances code quality, customizes code linting, ensures compatibility of dependencies, and detects bugs within the application flow.

By interfacing with a given GitHub repository, CodeInsighter employs advanced algorithms and analysis techniques to offer actionable insights and recommendations to developers. Through a user-friendly interface, it delivers real-time feedback on potential pitfalls, fosters better collaboration among team members, and empowers developers to make informed decisions, thereby significantly improving the overall quality and efficiency of software development processes.

## Pre-requisites
For build and running the application locally you would require:
- Java: [17.0.0](https://www.openlogic.com/openjdk-downloads?field_java_parent_version_target_id=807&field_operating_system_target_id=All&field_architecture_target_id=All&field_java_package_target_id=All)
- Maven: [4.0.0](https://dlcdn.apache.org/maven/maven-4/4.0.0-alpha-8/binaries/apache-maven-4.0.0-alpha-8-bin.zip)
- MySQL: [8.0.35](https://dev.mysql.com/downloads/installer/)

## Local Database Setup
- Open MySQL Workbench.
- Execute the SQL file - [LLM_insights.sql](https://git.cs.dal.ca/courses/2023-fall/csci-5308/Group12/-/blob/develop/login-authentication/src/main/resources/LLM_insights.sql)

## Running the Application Locally

- Clone this repository to your local machine:

```bash
  git clone https://git.cs.dal.ca/courses/2023-fall/csci-5308/Group12.git
```
- Edit this variables in /src/main/resources/application.properties
```bash
  //Database Connection Credentials
  spring.datasource.url= jdbc:mysql://localhost:3306/llminsights?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
  spring.datasource.username= <your-database-username>
  spring.datasource.password= <your-database-password>

  //Github OAuth Credentials
  spring.security.oauth2.client.registration.github.client-id=<your-oauth2-client_id>
  spring.security.oauth2.client.registration.github.client-secret=<your-oauth2-client_secret>

  //OpenAI Credentials
  openai.api.key= <your-openai-api-key>
``` 
- Change directory to the project's root directory:

```bash
  cd Group12
```

- Build the project using Maven:

```bash
  mvn clean package
```
- Run the application using Maven:

```bash
  mvn spring-boot:run
```

## Deployment Steps

#### Prerequisites
Before deploying the application, ensure you have the following prerequisites:
- Access to a server or host machine where the application will be deployed.
- Docker installed on the server.
- SSH access to the deployment server with necessary permissions.

#### Deployment Process

- **Build the Application**

Ensure the application is built using Maven. Run the following
command:
   ```bash
   mvn clean install
   ```

- **Publish Docker Image**

Push the Docker image to Docker Hub by executing the following commands:
   ```bash
  docker login -u YOUR_DOCKER_USERNAME -p YOUR_DOCKER_PASSWORD docker.io
  docker build -t docker.io/group12project/llm_insights:<VERSION> .
  docker push docker.io/group12project/llm_insights:<VERSION>
   ```
- **Deploy the Application**

SSH into the deployment server:
   ```bash
  ssh -i <path-to-your-private-key> YOUR_SERVER_USER@YOUR_SERVER_IP
   ```
Then run the following commands on the server to deploy the application:
```bash
docker login -u YOUR_DOCKER_USERNAME -p YOUR_DOCKER_PASSWORD docker.io
docker pull docker.io/group12project/llm_insights:<VERSION>
docker container rm -f my-app || true
docker run -d -p YOUR_DESIRED_PORT:8080 --name my-app docker.io/group12project/llm_insights:<VERSION>
   ```
Replace **<VERSION>** with the appropriate version or commit SHA and YOUR_DESIRED_PORT with the port number you want to expose the application on.

- **Verify the Deployment**
  
Access the deployed application via the browser using the server's IP address and the specified port.





## External Dependencies

| Dependency Name                      | Version    | Description                                                                 |
|--------------------------------------|------------|-----------------------------------------------------------------------------|
| spring-boot-starter-web              | 2.7.8      | Starter for building web applications using Spring MVC                      |
| spring-boot-starter-test             | 2.7.8      | Starter for testing Spring Boot applications                                |
| spring-boot-starter-oauth2-client    | 2.7.8      | Starter for OAuth2 client support in Spring Boot applications               |
| spring-security-oauth2-autoconfigure | 2.6.8      | Auto-configuration for Spring Security OAuth2                               |
| spring-boot-starter-thymeleaf        | 2.7.8      | Starter for building MVC web applications using Thymeleaf                   |
| js-cookie                            | 2.1.0      | JavaScript API for handling HTTP cookies                                    |
| webjars-locator-core                 |            | Library for locating web resources packaged as WebJars                      |
| lombok                               | 1.18.24    | Library to reduce Java boilerplate code by providing annotations            |
| spring-boot-starter-security         | 2.7.8      | Starter for enabling security features in Spring Boot applications          |
| spring-boot-starter-data-jpa         | 2.7.8      | Starter for using Spring Data JPA for database access                       |
| mysql-connector-java                 | 8.0.11     | MySQL JDBC driver                                                           |
| log4j-api                            | 2.5        | Logging API for Log4j 2                                                     |
| log4j-core                           | 2.11.2     | Core components for Log4j 2 logging framework                               |
| gson                                 | 2.10.1     | Java library to work with JSON                                              |
| jfreechart                           | 1.0.13     | Java library for creating various types of charts and graphs                |
| jsoup                                | 1.10.2     | Java library for working with HTML parsing and manipulation                 |

## Resources Used for CodeInsighter

- [UBOLD BootStrap Theme for Frontend](https://coderthemes.com/ubold/)
- [Jquery Plugin for Frontend](https://jquery.com/)
- [Intellij Idea Ultimate for development](https://www.jetbrains.com/idea/)
- [Codetabs for calculating lines of code](https://codetabs.com/count-loc/count-loc-online.html)
- [OpenAI API for LLM Insights](https://platform.openai.com/docs/guides/text-generation/chat-completions-api)

## User Scenarios

- **CodeInsighter Login**

![CodeInsighter Login](/reports/screenshots/CodeInsighterLogin.jpg)

- **Github OAuth Authentication**

User needs to login using the github credentials in order to get insights about the their repositories

![Github Login](/reports/screenshots/GithubLogin.jpg)

- **User Dashboard**

Upon successful Login user will see the repositories on the dashboard and they can access any respository

![User Dashboard](/reports/screenshots/UserDashboard.jpg)

- **Repository Statistics Dashboard**

On clicking any link of repository, user will be redirected to this page where they can see the statistics like Lines of code, No. of language used, pull requests and forks.

Also, the graphs of Portions of langauge used, No. of commits on each day of the week and finally the top Contributors of the repository are visible.

![Repository Statistics Dashboard](/reports/screenshots/RepositoryStatisticsDashboard.jpg)

- **LLM Insights**

On clicking Insights from the left menu bar of the Repository Statistics page, user will be redirected to this page. It will display 8 types of insights:

![InsightsDashboard](/reports/screenshots/InsightsDashboard.jpg)

**1) Common Code Mistakes:**
Here, the user will get insights based on common types of mistakes occurring in the code by using the code review comments.

![Common Code Mistakes](/reports/screenshots/CommonCodeMistakes.jpg)

**2) Collaboration Analysis:**
Here, the user will get insights on Collaboration Analysis. Using this feature, users can identify active contributors and their impact on the repository. This feature analyzes collaboration patterns, such as who works together frequently on issues and pull requests.

![Collaboration Analysis](/reports/screenshots/CollaborationAnalysis.jpg)

**3) Code Quality Enhancement:**
Here, the user will get insights on code improvements based on different criteria like Readability, Performance, Correctness, and Scalability. On open Pull Requests (PRs) by the first 3 developers who recently raised a PR, so that we can ensure code quality, adherence to coding standards, and catch potential issues early in the development process.

![Code Quality Enhancement](/reports/screenshots/CodeQualityEnhancement.jpg)

**4) Custom Code Linting:**
Here, the user will get insights on adherence to Linting Rules. This feature distinguishes itself from conventional code Linting by assessing code pushed by developers within open Pull Requests (PRs) and executing checks to ensure compliance with Linting standards.

![Custom Code Linting](/reports/screenshots/CustomCodeLinting.jpg)

**5) Dependency Version Compatibility:**
Here, the user will get insights based on Dependency Version Compatibility. Currently, it is limited for Java Maven projects that has pom.xml.

![Dependency Version Compatibility](/reports/screenshots/DependencyVersionCompatibility.jpg)

**6) Bug Detection in Application Flow:**
Here, the user will get insights on bug detection in application's Flow. Using this feature, users can identify application's major flaws. This featur  e is different from traditional code review. Here, the application will consider code pushed by the developer in any open PR that can introduce unexpected bugs in the application's major flows..

![Bug Detection in Application Flow](/reports/screenshots/BugDetectioninApplicationFlow.jpg)

**7) Test Case Minimization:**
Here, the user will get insights on Test Case Minimization. This feature will analyse trivial test cases in a repository. It will check test cases corresponding to each Pull Request and will provide insights about them.

![Test Case Minimization](/reports/screenshots/TestCaseMinimization.jpg)

**8) Advanced Code Search and Retrieval:**
Here, the user can efficiently locate specific code flows within any codebase.

![Advanced Code Search and Retrieval](/reports/screenshots/AdvancedCodeSearchandRetrieval.jpg)

## Contributors
- [Pooja Chauhan](https://git.cs.dal.ca/poojac) (B00971297)
- [Pratik Sakaria](https://git.cs.dal.ca/psakaria) (B00954261)
- [Nisarg Khacharia](https://git.cs.dal.ca/khacharia) (B00959791)
- [Sameer Amesara](https://git.cs.dal.ca/amesara) (B00961209)
- [Zeel Ravalani](https://git.cs.dal.ca/ravalani) (B00917373)

## Smell Analysis Summary

- [Excel File Attached]()

## Member Contribution and Client Feedbacks

- [Excel File Attached]()



