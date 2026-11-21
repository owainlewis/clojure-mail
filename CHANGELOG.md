# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed

- `has-flag?`, `read?`, `answered?`, `deleted?`, `draft?`, `flagged?` and `recent?` now detect system flags. They previously always returned false for messages because the flag name was checked as a user flag.
- `search-inbox` accepts the multi-condition queries shown in the README (`(search-inbox store :body "x" :from "y")`). It previously only accepted a single argument.
- `message-count`, `mark-all-read` and `clojure-mail.folder/with-folder` now close the folders they open instead of leaking open folders.
- `save-message-to-file` closes its output stream.
- `in-reply-to` works for messages read from files as well as IMAP messages.
- `mime-type` tolerates whitespace before the `;` in a content type, returns `nil` for `nil` input, and recognizes all `multipart/*` content types.
- `clojure-mail.message/uid` returns `nil` instead of throwing when a message is not backed by a `UIDFolder` (e.g. messages loaded from files).
- `clojure-mail.gmail/folder->folder-name` now returns string folder names unchanged instead of throwing `ClassCastException`.
- `clojure-mail.folder/build-search-terms` supports vector (OR-ed) values for `:to`, `:cc` and `:bcc`, validates empty collections and invalid dates, and ignores empty nested collections instead of throwing `NullPointerException`.
- `clojure-mail.parser/parse` no longer passes `"UTF-8"` as Jsoup's `baseUri`.
- `clojure-mail.events/new-idle-manager` uses daemon threads and `stop` shuts down its executor service; IDLE callbacks guard re-watching when the folder closes or the manager stops.
- `open-folder` throws a clear `IllegalArgumentException` on unknown permission levels; `xoauth2-store` accepts `[host port]` server vectors like `store`; `dump` realizes all futures before returning.
- Removed reflection warnings in library and test code.

### Added

- `clojure-mail.core/close-folder` and `clojure-mail.core/with-folder` helpers that close folders automatically on exit.
- Attachment helpers in `clojure-mail.message`: `attachment?`, `attachments` (with RFC 2047 filename decoding) and `save-attachment` (with path-traversal sanitization).
- Dropped-IDLE and connection lifecycle helpers in `clojure-mail.events`: `running?`, `connection-event->map`, `add-connection-listener`, and an optional `{:on-closed :on-disconnected :on-error}` map on `add-message-count-listener`.
- Two-argument arity `(save-message-to-file message path)` to write a message to an explicit path.
- `java.time.Instant` and `java.time.LocalDate` support in `clojure-mail.folder/to-date`.
- Additional Gmail folder shortcuts in `clojure-mail.gmail/gmail-folders` (`:drafts`, `:important`, `:starred`, `:trash`).
- `mark-draft`. `mark-as-flagged` is deprecated: it always set DRAFT, not FLAGGED. Its behaviour is unchanged.
- `has-flag?` accepts keywords (`:seen`), names (`"SEEN"`), `Flags$Flag` values and user flag names.
- GitHub Actions CI on Java 11, 17 and 21 with Clojure 1.11 and 1.12, formatting, linting, packaging and dependency scanning.
- Tag-gated Clojars release workflow and automated GitHub Release / changelog generation (`.github/release.yml`, `.github/workflows/changelog.yml`).

### Changed

- Search terms are built directly instead of through `eval`.
- Dependencies: Clojure 1.12.7 (fixes GHSA-x4c4-68gp-vrhc; Clojure 1.11.4 remains tested in CI), jsoup 1.24.1, Jakarta Mail 1.6.8, and `dev.weavejester/lein-cljfmt` 0.16.6. Updates fix known vulnerabilities (GHSA-x4c4-68gp-vrhc, GHSA-9342-92gg-6v29, GHSA-65r4-943x-97jj, GHSA-pmhh-3w7g-xqp8). Dropped the `medley` dependency.
- README rewritten with Gmail app password and OAuth guidance.
- Project URL now points at `github.com/owainlewis/clojure-mail`.

<!-- auto-changelog:start -->
<!-- Automatically populated from merged PRs on push to master -->
<!-- auto-changelog:end -->

## [1.0.8]

Last release before this changelog was started.
