package com.taim.conduire.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

//TODO --> Design Smell: Broken Modularization
@Entity
@Table(name = "user_repos")
@Getter @Setter @ToString
public class RepoData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Access(AccessType.PROPERTY)
    private Integer id;

    @Column(name = "github_repo_id")
    private Integer githubRepoId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "private")
    private Boolean isPrivate;

    @Column(name = "fork")
    private Boolean isFork;

    @Column(name = "size")
    private Integer size;

    @Column(name = "has_issues")
    private Boolean hasIssues;

    @Column(name = "has_projects")
    private Boolean hasProjects;

    @Column(name = "has_downloads")
    private Boolean hasDownloads;

    @Column(name = "has_wiki")
    private Boolean hasWiki;

    @Column(name = "forks_count")
    private Integer forksCount;

    @Column(name = "forks")
    private Integer forks;

    @Column(name = "open_issues")
    private Integer openIssues;

    @Column(name = "open_issues_count")
    private Integer openIssuesCount;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(name = "language")
    private String language;

    @Column(name = "repo_created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date repoCreatedAt;

    @Column(name = "repo_updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date repoUpdatedAt;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

}
