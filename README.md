<img src="src/main/resources/static/favicon.svg" alt="Surok" height="64">

[![Build](https://github.com/eshabakhov/surok/actions/workflows/build.yml/badge.svg)](https://github.com/eshabakhov/surok/actions/workflows/build.yml)
[![Deploy](https://github.com/eshabakhov/surok/actions/workflows/deploy.yml/badge.svg)](https://github.com/eshabakhov/surok/actions/workflows/deploy.yml)
[![trufflehog-oss](https://github.com/eshabakhov/surok/actions/workflows/trufflehog-oss.yml/badge.svg)](https://github.com/eshabakhov/surok/actions/workflows/trufflehog-oss.yml)

[![actionlint](https://github.com/eshabakhov/surok/actions/workflows/actionlint.yml/badge.svg)](https://github.com/eshabakhov/surok/actions/workflows/actionlint.yml)
[![yamllint](https://github.com/eshabakhov/surok/actions/workflows/yamllint.yml/badge.svg)](https://github.com/eshabakhov/surok/actions/workflows/yamllint.yml)
[![shellcheck](https://github.com/eshabakhov/surok/actions/workflows/shellcheck.yml/badge.svg)](https://github.com/eshabakhov/surok/actions/workflows/shellcheck.yml)

[![Lines-of-Code](https://img.shields.io/tokei/lines/github/eshabakhov/surok)](https://github.com/eshabakhov/surok)
[![Hits-of-Code](https://hitsofcode.com/github/eshabakhov/surok)](https://hitsofcode.com/view/github/eshabakhov/surok)

[![Version](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2Feshabakhov%2Fsurok%2Fmain%2Fpom.xml&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27version%27%5D&label=Version&color=111827)](pom.xml)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE.txt)

**Surok** is a Java web application for planning school schedules. It manages
school buildings, classrooms, classes, subjects, teachers, and class workloads,
and includes a catalog of federal curricula.

Install [Git] and [Docker Desktop] with Linux containers, or Docker Engine with
Compose v2.24+. Start Docker, then run:

```sh
git clone https://github.com/eshabakhov/surok.git
cd surok
docker compose up --build
```

Open [Surok](http://localhost:19500/) and [create an account] for your school.
Docker builds the application and starts PostgreSQL with local defaults.

## Configuration

To customize the local setup, copy [.env.example](.env.example) to `.env` and
edit the settings you need, such as `APP_PORT` or the database credentials.
The same file lists OAuth, administrator, and monitoring settings.

To create the first administrator, set `BOOTSTRAP_ADMIN_ENABLED=true` and fill
in `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL`, and
`BOOTSTRAP_ADMIN_PASSWORD` in `.env`. Run `docker compose up --build`, sign in
with those credentials, then set `BOOTSTRAP_ADMIN_ENABLED=false` for subsequent
starts. Administrators can manage schools and the federal curriculum catalog.

With `APP_SECURITY_DOCS_ENABLED=true`, `/swagger-ui.html` opens without login.
It initially shows public operations. **Authorize** verifies the account's
username and password and loads the operations permitted for that user;
**Logout** restores the public description. Swagger authentication is separate
from the website session. School-specific access is checked on every API request.
Credentials remain in memory until logout or page reload; use HTTPS outside
local development. **Try it out** includes the application's CSRF token.

Maintain the OpenAPI description in `src/main/resources/openapi.yml`.
The server filters that document by the current user's permissions for
`/api-docs` (JSON) and `/api-docs.yaml` (YAML).

Administrators can open `/permissions` to inspect resource access by role.
The page supports searching actions and endpoints; `✓*` means access within the
user's school. JSON and XML representations are selectable in Swagger UI.

Run `docker compose down` to stop the local stack. Database data is preserved
in a Docker volume.

## Deployment

The [Build workflow](.github/workflows/build.yml) checks pull requests and
publishes images from `main` to GHCR with commit SHA and `latest` tags.
The [Deploy workflow](.github/workflows/deploy.yml) deploys the image for a
successful build on `main`. To redeploy an existing image, run Deploy manually
with its full commit SHA in `revision`.

Production services are defined by the `production` profile in
[docker-compose.yml](docker-compose.yml). Deployment configuration and required
GitHub secrets are listed in the Deploy workflow.

## How to contribute

Start with an [issue] describing the problem or proposed change. Work in a
separate branch and submit a pull request to `main` with a description of the
change and how you verified it.

For local checks, install [JDK 21], set `JAVA_HOME` to that installation, and
keep Docker running for the PostgreSQL tests. From the repository root, run:

```sh
./mvnw -ntp --batch-mode clean verify -Pqulice
```

Maven Wrapper downloads Maven; the build installs Node.js and npm, runs tests,
checks Java with Qulice, and lints JavaScript and CSS. On Windows, use
`mvnw.cmd` instead of `./mvnw`.

To run a single test class:

```sh
./mvnw -Dtest=SlInputNameTest test
```

To run Java on your computer with PostgreSQL in Docker:

```sh
docker compose up -d postgres
./mvnw clean spring-boot:run
```

The database defaults in [application.yml] match Compose. If you customize
database settings in `.env`, pass the same `DB_*` variables to Maven or your
IDE: Compose reads `.env`, while a direct Java launch uses its environment.
For local HTTP login, set `APP_SECURITY_SECURE_COOKIE=false`. Yandex login
requires real `YA_ID` and `YA_SECRET`; local defaults support password login.

Before launching `SurokApplication` from an IDE, run `./mvnw process-classes`
to generate build metadata and prepare XSLT resources in `target/classes`.

JavaScript tests and style checks can also run separately with `npm ci`,
`npm test`, and `npm run lint`.

After editing the application, run `docker compose up --build` again to see
the changes. If a command fails, include the command and its log in the issue.
Pull requests run CI checks before review.

[Git]: https://git-scm.com/downloads/
[Docker Desktop]: https://docs.docker.com/desktop/
[JDK 21]: https://adoptium.net/temurin/releases/?version=21
[create an account]: http://localhost:19500/users/register
[issue]: https://github.com/eshabakhov/surok/issues
[application.yml]: src/main/resources/application.yml
