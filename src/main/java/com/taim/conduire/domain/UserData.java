package com.taim.conduire.domain;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "users")
@Getter @Setter @ToString
public class UserData implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Access(AccessType.PROPERTY)
	private Integer id;

	@Column(name = "github_user_id")
	private Integer githubUserId;

	@Column(name = "name")
	private String name;

	@Column(name = "user_name")
	private String userName;

	@Column(name = "user_access_token")
	private String userAccessToken;

	@Column(name = "email")
	private String email;

	@Column(name = "account_type")
	private String accountType;

	@Column(name = "avatar_url")
	private String avatarUrl;

	@Column(name = "company")
	private String company;

	@Column(name = "location")
	private String location;

	@Column(name = "bio")
	private String bio;

	@Column(name = "twitter_username")
	private String twitterUsername;

	@Column(name = "public_repos")
	private Integer publicRepos;

	@Column(name = "followers")
	private Integer followers;

	@Column(name = "private_owned_repos")
	private Integer privateOwnedRepos;

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Column(name = "last_visited_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastVisitedOn;

	@Column(name = "collaborators")
	private Integer collaborators;

	@Column(name = "visible")
	private char visible;

}
