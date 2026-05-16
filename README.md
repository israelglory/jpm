# JPM (Java Package Manager)

JPM is a Java CLI tool that adds Maven dependencies to `pom.xml` using simple aliases.

## MVP commands

```bash
jpm add postgres
jpm add lombok
jpm add web
```

## What it does

- Parses terminal commands
- Detects `pom.xml`
- Resolves aliases to Maven coordinates
- Fetches the latest version from Maven Central
- Updates `pom.xml` using DOM XML parsing
- Prevents duplicate direct dependencies
- Saves the modified file safely

## Project structure

```text
src/main/java/com/jpm/
├── Main.java
├── Dependency.java
├── DependencyResolver.java
├── MavenCentralClient.java
└── PomEditor.java
```

## Why the classes exist

- `Main.java` – CLI entrypoint, command parsing, and user-facing messages
- `Dependency.java` – immutable dependency model
- `DependencyResolver.java` – maps aliases to real Maven coordinates and asks Maven Central for the latest version
- `MavenCentralClient.java` – performs the HTTP request to Maven Central
- `PomEditor.java` – reads and updates `pom.xml` with DOM APIs

## Build requirements

- Java 17+
- Maven 3.9+

## Compile

```bash
cd /Users/user/Documents/Backend/jpm
mvn clean package
```

## Run locally

From the directory that contains your Maven project `pom.xml`:

```bash
java -jar target/jpm-1.0.0.jar add postgres
```
Or use the local launcher:

```bash
./bin/jpm add postgres
```

Or run directly from compiled classes:

```bash
mvn -q compile
java -cp target/classes Main add lombok
```

## Package into an executable JAR

```bash
mvn clean package
```

The JAR will be at `target/jpm-1.0.0.jar`.

## Installation & Distribution

### From GitHub Releases (recommended)

Use the automated installer:

```bash
curl -sSL https://raw.githubusercontent.com/your-org/jpm/main/install-jpm.sh | bash -s -- v1.0.0
```

Or specify `latest`:

```bash
curl -sSL https://raw.githubusercontent.com/your-org/jpm/main/install-jpm.sh | bash -s -- latest
```

### Homebrew (if you set up a tap)

```bash
brew tap your-org/jpm https://github.com/israelglory/homebrew-jpm
brew install your-org/jpm/jpm
```

## Release Workflow

1. Update `CHANGELOG.md` with new version and changes.
2. Update `pom.xml` version (e.g., `1.0.0` for releases, `1.0.1-SNAPSHOT` for dev).
3. Commit and push:
   ```bash
   git add CHANGELOG.md pom.xml
   git commit -m "Release v1.0.0"
   git tag -a v1.0.0 -m "JPM v1.0.0"
   git push origin main
   git push origin v1.0.0
   ```

4. GitHub Actions automatically:
   - Builds the JAR
   - Generates checksums
   - Creates a GitHub Release with JAR and checksums attached

5. Verify the release on [GitHub Releases](https://github.com/your-org/jpm/releases).

## Version

Check the version:

```bash
jpm --version
# or
jpm version
```

## Support & Contributions

- Report issues on [GitHub Issues](https://github.com/your-org/jpm/issues)
- See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines
- See [CHANGELOG.md](CHANGELOG.md) for release history

The JAR will be created in `target/` and contains a manifest that points to `Main`.

## Expected output

Success:

```text
✔ Dependency added successfully
```

Duplicate:

```text
✔ Dependency already exists
```

Missing `pom.xml`:

```text
✘ No pom.xml found
```

[//]: # ()
[//]: # (## Suggested MVP improvements)

[//]: # ()
[//]: # (1. Add support for direct coordinates like `jpm add org.slf4j:slf4j-api`)

[//]: # (2. Add more aliases and categories)

[//]: # (3. Support plugin dependencies and `dependencyManagement`)

[//]: # (4. Add offline caching for Maven Central responses)

[//]: # (5. Add tests for XML edge cases and HTTP failures)

[//]: # (6. Preserve original formatting more closely when rewriting `pom.xml`)

[//]: # (7. Add a dedicated `--version` and `--help` command)

[//]: # (8. Add update/remove commands)

