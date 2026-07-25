# AGENTS.md

## Working Agreement

- Do not modify files unless the user explicitly asks for a change.
- Treat questions as requests for concise answers, not implementation work.
- Do not provide code examples unless the user asks for them.
- Ask before adding dependencies, changing architecture, or expanding requested scope.
- Do not add tests or run build, test, lint, format, or application commands unless explicitly requested.
- Preserve unrelated worktree changes. Never revert changes made by the user.

## Project Overview

Morning Coffee is a small Java RSS/Atom reader. It polls configured feeds, stores entries, and serves a plain HTML page containing recent entries.

- Java: 25
- Build: Maven
- Entry point: `ee.fakeplastictrees.morningcoffee.App`
- Source root: `src/main/java`
- Resources: `src/main/resources`
- Database design: `spec/schema.sql`

## Configuration

Application reads configuration from environment variables. See `ee.fakeplastictrees.morningcoffee.Config` for more details.

## Package Boundaries

Organize code by feature, not generic `interfaces` and `impl` layers.

- Root package: application composition and configuration only.
- `reader`: feed scheduling, HTTP retrieval, RSS parsing, and mapping feed entries.
- `webserver`: HTTP serving, user-facing page rendering, HTML output encoding, and static web resources.
- `repository`: persistence and database-specific clients, including PostgreSQL access.
- `model`: shared domain records without HTTP, ROME, HTML, or database dependencies.

## Design Guidance

- Prefer smallest concrete design that solves current requirement.
- Do not introduce interfaces solely for hypothetical replacement or testing needs.
- Keep public API surface minimal; use package-private classes for package internals.
- Use virtual threads for independent blocking I/O, while keeping schedulers separate from worker executors.
- Repository operations must remain safe under concurrent feed writes and web reads.

## Java Style

- Follow Google Java Style and existing source conventions.
- Prefer `var` for readable local declarations.
- Preserve exception causes and add operation-specific context to exception messages.
- Use Log4j parameterized messages instead of pre-formatting log strings.
- Avoid speculative helpers, abstractions, and compatibility code.

## Security Boundaries

- Treat feed data as untrusted external input.
- ROME parses feeds but does not make values safe for HTML.
- Encode titles as HTML content and links as HTML attributes at rendering time.
