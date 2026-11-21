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
<!-- Release notes generated using configuration in .github/release.yml at master -->

### What's Changed
#### Enhancements
* Improve library reliability, test coverage, dependencies, and automated changelog by @owainlewis in https://github.com/owainlewis/clojure-mail/pull/81
#### Bug Fixes
* Fixes by @kwieloch in https://github.com/owainlewis/clojure-mail/pull/65
#### Other Changes
* adds basic IMAP UID support by @erjoalgo in https://github.com/owainlewis/clojure-mail/pull/47
* folder search query syntax is extended by @jozseftiborcz in https://github.com/owainlewis/clojure-mail/pull/50
* Update README.md by @feifanzhou in https://github.com/owainlewis/clojure-mail/pull/51
* Bumped javax.mail dependency to 1.5.5 and clojure-mail version to 1.0.6 by @nrakochy in https://github.com/owainlewis/clojure-mail/pull/56
* Update how content types are handled by @skammer in https://github.com/owainlewis/clojure-mail/pull/58
* Add ability to mark a single message as read by @skammer in https://github.com/owainlewis/clojure-mail/pull/57
* update README file by @quan-nh in https://github.com/owainlewis/clojure-mail/pull/59
* Bumped medley 1.0 by @fvides in https://github.com/owainlewis/clojure-mail/pull/61
* Close FileInputStream in file->message by @amoe in https://github.com/owainlewis/clojure-mail/pull/62
* Bump version / I want the Medley upgrade by @hacklschorsch in https://github.com/owainlewis/clojure-mail/pull/68
* Add several missing flags to allow deletion, drafting, etc by @cguckes in https://github.com/owainlewis/clojure-mail/pull/72
* Update README.md in https://github.com/owainlewis/clojure-mail/pull/73
* chore: add pull request template by @owainlewis in https://github.com/owainlewis/clojure-mail/pull/78
* Modernize clojure-mail: bug fixes, CI, docs and maintainer charter by @owainlewis in https://github.com/owainlewis/clojure-mail/pull/79

### New Contributors
* @erjoalgo made their first contribution in https://github.com/owainlewis/clojure-mail/pull/47
* @jozseftiborcz made their first contribution in https://github.com/owainlewis/clojure-mail/pull/50
* @feifanzhou made their first contribution in https://github.com/owainlewis/clojure-mail/pull/51
* @nrakochy made their first contribution in https://github.com/owainlewis/clojure-mail/pull/56
* @skammer made their first contribution in https://github.com/owainlewis/clojure-mail/pull/58
* @quan-nh made their first contribution in https://github.com/owainlewis/clojure-mail/pull/59
* @fvides made their first contribution in https://github.com/owainlewis/clojure-mail/pull/61
* @amoe made their first contribution in https://github.com/owainlewis/clojure-mail/pull/62
* @kwieloch made their first contribution in https://github.com/owainlewis/clojure-mail/pull/65
* @hacklschorsch made their first contribution in https://github.com/owainlewis/clojure-mail/pull/68
* @cguckes made their first contribution in https://github.com/owainlewis/clojure-mail/pull/72

**Full Changelog**: https://github.com/owainlewis/clojure-mail/commits/v1.1.0
<!-- auto-changelog:end -->

## [1.0.8]

Last release before this changelog was started.
