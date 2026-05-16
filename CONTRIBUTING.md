# Contributing to JPM

Thank you for your interest in JPM! We welcome contributions. Here's how to get started.

## Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/jpm.git
   cd jpm
   ```

2. Build locally:
   ```bash
   mvn clean package
   ```

3. Test a local build:
   ```bash
   java -jar target/jpm-1.0.0-SNAPSHOT.jar add postgres
   ```

## Code Style

- Follow standard Java conventions (camelCase, meaningful names).
- Add Javadoc to public APIs.
- Keep classes focused and small (single responsibility).
- Use standard Java APIs only (no external dependencies in MVP).

## Making Changes

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature
   ```

2. Make your changes and commit with clear messages:
   ```bash
   git commit -m "Add support for X"
   ```

3. Test:
   ```bash
   mvn clean test
   ```

4. Push and open a pull request.

## Release Process

1. Update `CHANGELOG.md` with the new version.
2. Update `pom.xml` with the new version (remove -SNAPSHOT for releases).
3. Tag and push:
   ```bash
   git tag -a v1.X.X -m "Release v1.X.X"
   git push origin v1.X.X
   ```

GitHub Actions will automatically build and create a release.

## Questions?

Open an issue or discussion on GitHub. We're happy to help!

