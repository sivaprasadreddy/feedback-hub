# FeedbackHub

[![Build](https://github.com/sivaprasadreddy/feedback-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/sivaprasadreddy/feedback-hub/actions/workflows/ci.yml)

A feedback management platform where an organization can provide their employees with a private space to:

* Express feedback, opinions, suggestions, concerns, or ideas.
* Participate in discussions through replies.
* Post or reply using their identity or anonymously.
* Upvote/downvote messages and replies.
* Discover popular and recent discussions.
* Allow organization administrators to manage users.

## Tech Stack

* Java
* Spring Boot
* Spring Modulith
* Spring AI, Ollama
* Spring Security
* Spring Data JPA
* PostgreSQL
* FlywayDb
* Thymeleaf
* Tailwind CSS4

## Prerequisites
* JDK 25
* Docker and Docker Compose
* [Ollama](https://ollama.com/) with [gemma3:270m](https://ollama.com/library/gemma3) model
* Your favourite IDE (Recommended: [IntelliJ IDEA](https://www.jetbrains.com/idea/))

Follow the [Installation Guide](docs/installation.md) to install the required tools.

Verify the prerequisites:

```shell
$ java -version
$ docker info
$ docker compose version
$ task --version
$ ollama --version
```

## How to run?

```shell
# runs all tests
$ task test 

# formats java code using spotless
$ task format

# builds docker image
$ task build_image 

# starts app using docker compose
$ task start 
$ task stop
$ task restart
```

* Application URL: http://localhost:8080
* Credentials: `admin@gmail.com/secret`, `siva@gmail.com/secret`

## Deploying on k8s cluster

Set up a Kind cluster following [Installation Guide](docs/installation.md)

```shell
# create kind cluster 
$ task kind_create

# deploy app to kind cluster 
$ task k8s_deploy  // this will take a while to download ollama

# undeploy app
$ task k8s_undeploy

# destroy kind cluster 
$ task kind_destroy
```

Application URL: http://localhost:80

## Using Agent Skills

This repository includes project-specific Agent Skills in [`.agents/skills`](.agents/skills). 

```text
Use $prd-to-requirements to generate docs/requirements.md from docs/prd.md.
Use $implement-usecase to implement UC-001.
Use $update-usecase to update UC-001 from the current implementation.
Use $java-code-review to review the modified Java files.
```

| Skill                  | Purpose                                                                       |
|------------------------|-------------------------------------------------------------------------------|
| `$prd-to-requirements` | Generate or regenerate use-case requirements from the PRD.                    |
| `$implement-usecase`   | Implement and test one `UC-###`, updating its status during the workflow.     |
| `$update-usecase`      | Synchronize one use case with behavior already present in the code and tests. |
| `$java-code-review`    | Review Java changes and create an actionable `review.md` report.              |

You may want to install additional skills from [sivalabs-agent-skills](https://github.com/sivaprasadreddy/sivalabs-agent-skills).
