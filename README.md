# SpeakUp
A feedback management platform where an organization can provide their employees with a private space to:

* Express feedback, opinions, suggestions, concerns, or ideas.
* Participate in discussions through replies.
* Post or reply using their identity or anonymously.
* Upvote/downvote messages and replies.
* Discover popular and recent discussions.
* Allow organization administrators to manage users.

## Prerequisites
* JDK 25
* Docker and Docker Compose
* Your favourite IDE (Recommended: [IntelliJ IDEA](https://www.jetbrains.com/idea/))

Install JDK, Maven, Gradle using [SDKMAN](https://sdkman.io/)

```shell
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk env install
```

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
