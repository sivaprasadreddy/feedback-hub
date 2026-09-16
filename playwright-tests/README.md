# Playwright Browser Tests

These tests drive running feedbackhub application in a real browser:

* `LoginAndPostMessageTests` - Login as admin/user and post a message.

## Prerequisites

Start feedbackhub from the repository root:

```bash
./mvnw -pl feedbackhub spring-boot:run -Dspring-boot.run.profiles=local
```

Install the Chromium browser once:

```bash
./mvnw -pl playwright-tests -Dexec.classpathScope=test \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  org.codehaus.mojo:exec-maven-plugin:java -Dexec.args="install chromium"
```

## Run the tests

```bash
./mvnw -pl playwright-tests test -Dplaywright.skip=false
```

The tests target `http://localhost:8080` by default. Override it when needed:

```bash
./mvnw -pl playwright-tests test \
  -Dplaywright.skip=false \
  -DbaseUrl=https://feedbackhub.com
```

The module skips browser tests during the default reactor build because it requires a separately running application.