# Installation Guide

## SDKMAN
Install JDK, Maven, Gradle, etc using [SDKMAN](https://sdkman.io/)

```shell
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk install java 25-tem
$ sdk install maven
$ sdk install gradle
```

## Taskfile
Task is a task runner that we can use to run any arbitrary commands in an easier way.

```shell
$ brew install go-task
(or)
$ go install github.com/go-task/task/v3/cmd/task@latest
```

## Ollama
Download Ollama installer for your OS at https://ollama.com/.

Once installed and started the service, visiting http://localhost:11434/ should return `Ollama is running`.

From the terminal pull/run the models:

```shell
$ ollama help
$ ollama pull gemma3:270m
$ ollama run gemma3:270m
$ ollama list
$ ollama ps
```

## Kind Cluster
* [Install kubectl](https://kubernetes.io/docs/tasks/tools/)
* [Install kind](https://kind.sigs.k8s.io/docs/user/quick-start/)

```shell
$ brew install kubectl
$ brew install kind
```

Create a KinD cluster.

```shell
# Create KinD cluster
$ task kind_create

# Destroy KinD cluster
$ task kind_destroy
```
