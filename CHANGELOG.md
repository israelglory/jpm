# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-05-16

### Added
- Initial MVP release of JPM (Java Package Manager).
- CLI command `jpm add <alias>` to add Maven dependencies to `pom.xml`.
- Support for predefined aliases: `postgres`, `lombok`, `web`.
- Free-text search on Maven Central when alias is not found.
- Interactive selection when multiple search results are returned.
- DOM-based XML editing to safely modify `pom.xml`.
- Duplicate dependency detection.
- Network retry logic with exponential backoff for Maven Central lookups.
- Help command: `jpm help`.
- Version command: `jpm --version`.

