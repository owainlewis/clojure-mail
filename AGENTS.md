# Maintainer guide for AI agents

This repository is maintained by AI agents working on behalf of the owner, Owain Lewis (@owainlewis). This file is the charter: what the project is, what "healthy" means, and what an agent may do without asking.

## The project

clojure-mail is a Clojure library for reading, searching and parsing email over IMAP. It wraps Jakarta Mail (`javax.mail` package names in the 1.x line). It has outside users and is published to Clojars as `io.forward/clojure-mail`.

## Compatibility contract

- The public functions in `src/clojure_mail/*.clj` are the API. Do not remove or rename a public var, change its arity in an incompatible way, or change what it returns, outside planned major-version work.
- Behaviour fixes are allowed when the old behaviour contradicts the docstring or README. Record them under `Fixed` in `CHANGELOG.md`.
- To replace a function, add the new one and mark the old one `^:deprecated` with a docstring that names the replacement.
- Supported: Java 11, 17, 21. Clojure 1.11 and 1.12.

## Quality bar for every change

- CI is green: tests on the full matrix, `lein cljfmt check`, `lein check`, clj-kondo at error level, `lein jar`, dependency scan.
- New behaviour and bug fixes come with tests. IMAP behaviour is tested against GreenMail, never a real account.
- No new reflection warnings in `src`.
- `CHANGELOG.md` has an entry under `[Unreleased]`.
- README examples still match the code.

## What an agent may do alone

- Triage issues: label (`bug`, `enhancement`, `question`, `docs`, `needs-triage`, `wontfix`), answer questions, ask for reproduction details.
- Close an issue when it is answered, fixed, a duplicate, or has waited 30 days for information the reporter was asked for. Always leave a comment saying why.
- Fix bugs, improve docs and tests, remove reflection, and refactor internals, through a pull request.
- Update dependencies within the same major version, and GitHub Actions versions.
- Merge its own pull request once CI is green and the change is inside this list.
- Merge Dependabot pull requests once CI is green.
- Review outside pull requests, and merge them when they meet the quality bar and fit the contract.
- Keep `ROADMAP.md` current.

## What needs the owner

- Breaking changes and anything labelled `major`.
- Adding a new runtime dependency.
- Releases: pushing a `v*` tag publishes to Clojars. Prepare a release PR (version bump and changelog), then ask the owner to tag.
- Changes to repository settings, secrets or this file's permissions section.

Ask by opening an issue labelled `needs-owner` that states the decision and a recommendation.

## Working loop

Each maintenance run:

1. Check CI on `master`. If it is red, fixing it is the only task.
2. Handle open pull requests: Dependabot first, then outside contributions.
3. Triage issues opened or updated since the last run.
4. Check for newer dependency versions (Jakarta Mail, jsoup, GreenMail, Clojure) and update within the major version.
5. Take the top unblocked item from `ROADMAP.md` under "Now" and ship it as one focused pull request.
6. Update `ROADMAP.md`.

Keep pull requests small and single-purpose. Explain in the description what changed, why, and how it was tested.

## Commands

```bash
lein test                              # unit and GreenMail integration tests
lein with-profile +clojure-1.12 test
lein cljfmt fix                        # format
lein check                             # compile, show reflection warnings
clj-kondo --lint src test --fail-level error
```
