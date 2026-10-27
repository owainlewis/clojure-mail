# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed

- `has-flag?`, `read?`, `answered?`, `deleted?`, `draft?`, `flagged?` and `recent?` now detect system flags. They previously always returned false for messages because the flag name was checked as a user flag.
- `search-inbox` accepts the multi-condition queries shown in the README (`(search-inbox store :body "x" :from "y")`). It previously only accepted a single argument.
- `message-count` closes the folder it opens instead of leaking an open folder per call.
- `save-message-to-file` closes its output stream.
- `in-reply-to` works for messages read from files as well as IMAP messages.
- `mime-type` tolerates whitespace before the `;` in a content type.
- Removed reflection warnings in library code.

### Added

- `mark-draft`. `mark-as-flagged` is deprecated: it always set DRAFT, not FLAGGED. Its behaviour is unchanged.
- `has-flag?` accepts keywords (`:seen`), names (`"SEEN"`), `Flags$Flag` values and user flag names.
- GitHub Actions CI on Java 11, 17 and 21 with Clojure 1.11 and 1.12, formatting, linting, packaging and dependency scanning.
- Tag-gated Clojars release workflow.

### Changed

- Search terms are built directly instead of through `eval`.
- Dependencies: Clojure 1.11.4, jsoup 1.21.2, Jakarta Mail 1.6.7. Dropped the `medley` dependency.
- README rewritten with Gmail app password and OAuth guidance.
- Project URL now points at `github.com/owainlewis/clojure-mail`.

## [1.0.8]

Last release before this changelog was started.
