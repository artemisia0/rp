### Rocnikovy projekt (2 rocnik)
#### Topic: Data changes monitoring in apache iceberg

Project is under heavy development now.

#### Project structure
Main idea is that ```rp``` is a monorepo that contains frontend and backend and in order to deploy the whole app it would be enough just to deploy the backend only. Whole project is basically a single page application (interactive web app, SPA, no need for SSR, SEO) and a backend API/server in java.

Directory structure:
```
rp

  frontend
    package.json
    vite.config.js
    src
    dist  # generated after build

  iceberg_app
    Dockerfile
    pom.xml
    src
      main
        resources
          static  # contains built frontend files

  docker-compose.yml
  README.md
```

#### What a project should do and how
Collect all neccessary info about apache iceberg db after connecting to it by a link/url. Send some data a client requires with respect to given filters provided by the client. Then the client visualizes data and provides a convenient interface for a user to do some useful stuff.


#### Tech stack & stuff
Frontend:
- vue
Backend:
- spring
- some database, probably sqlite

#### Deploy (and different related stuff...)
Ideally only the backend container (or jar) should be deployed and backend would already include the built frontend. So the app would work as a single deployable unit. Docker will also be used for more convenience. Frontend content will be server from spring boot (```src/main/resources/static/``` is automatically served at the root ```/```). So it is enough to copy output (```frontend/dist/```) to ```backend/src/main/resources/static/```. API is under ```/api/...```.

#### Commit messages format and git usage guidelines
commit type(commit scope): short summary

Examples:
- feat(auth): add authentication for login api

Common commit types:
- feat     --> new feature
- fix      --> bug fix
- docs     --> documentation changes
- style    --> formatting changes only
- refactor --> no new features, no bug fixes
- perf     --> performance improvement
- test     --> adding/changing tests (doctest for doctests)
- build    --> changes to build system, deps, tools
- ci       --> changes to ci pipeline
- chore    --> other stuff that does not affect code (e.g. deps update, readme update)
- revert   --> reverting a commit (undo in a safer way)

Scope stands for a part of a repo that commit affects most (e.g. some dashboard, package).

Other conventions:
- use imperative style for verbs: add instead of added or adding.
- keep it short: <= 72 characters.
- lowercase the first letter.
- no period at the end.

Example:
- OK: fix(auth): handle expired tokens
- NO: Fixed an issue where tokens were expiring too early.

Some more general guidelines:
- one change per one commit (NO fat commits)
- main branch is always deployable and passes tests
- development is in separate branches (merge to main only on passing all tests and so on)
- each new task (feature, fix, refactor) gets its own branch

