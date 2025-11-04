### Rocnikovy projekt (2 rocnik)
#### Topic: Data changes monitoring in apache iceberg

Project is under heavy development now.

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

