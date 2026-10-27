# Contributing

## Local checks

Install Java 11 or newer, Leiningen 2.11.2 or newer, and clj-kondo.

```bash
lein test
lein with-profile +clojure-1.12 test
lein cljfmt check
lein check
clj-kondo --lint src test --fail-level error
lein jar
```

Tests must not need a real mail account. Use the GreenMail helpers in `test/clojure_mail/helpers` for IMAP behaviour, and the fixtures in `dev-resources/emails` for parsing.

## Pull requests

- Keep the public API of `clojure-mail.core`, `clojure-mail.message`, `clojure-mail.folder`, `clojure-mail.gmail`, `clojure-mail.events` and `clojure-mail.parser` backwards compatible unless the change is planned major-version work.
- Add a test for every bug fix and feature.
- Add an entry under `[Unreleased]` in `CHANGELOG.md`.
- Never put credentials or tokens in tests, logs or exceptions.

## Releases

Releases are tag-gated.

1. Set a non-snapshot version in `project.clj` and move the `[Unreleased]` changelog entries under that version.
2. Merge the release commit.
3. Push a matching `vX.Y.Z` tag.
4. The protected `clojars` GitHub environment publishes after checking that the tag and project version match. It needs the `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` secrets.
