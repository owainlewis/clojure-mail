# Roadmap

Maintained by the project's agents and the owner. See `AGENTS.md` for how items move.

## Now

- Release 1.1.0 from the current `master` (needs the owner to tag).
- Triage the open issue backlog: answer or close the questions from 2015-2020, keep the real bugs.

## Next

- Clear exceptions when a store cannot connect or authenticate (#28).
- A `send` namespace for SMTP, so the library covers sending as well as reading.

## Later: 2.0.0 (breaking)

- Move to Jakarta Mail 2 (`jakarta.mail` package names, Eclipse Angus implementation). Users who import `javax.mail` classes must update their imports.
- `message-body` always returns a sequence of parts, single-part messages included (#67).
- Return Clojure data (maps) by default and keep raw Java objects behind explicit functions.

## Done

- 2026-11: IDLE drop detection and connection listeners (#71), `with-folder`/`close-folder` leak-free helpers (#60), attachment helpers (`attachments`, `save-attachment`), bug fixes in `gmail`/`folder`/`parser`/`events`, dependency refresh (Clojure 1.12.7, jsoup 1.24.1, `dev.weavejester/lein-cljfmt` 0.16.6), expanded unit and GreenMail test coverage.
- 2026-10: CI on GitHub Actions, flag detection fix, `search-inbox` multi-condition queries, folder leak in `message-count`, README rewrite, dependency refresh.
