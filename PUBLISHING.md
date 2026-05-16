# JPM Publishing and Release Guide

This guide shows you how to publish JPM on GitHub and create automated releases.

## Prerequisites

- GitHub account
- Git installed locally
- GitHub CLI (optional, but recommended): `brew install gh` (macOS) or see https://github.com/cli/cli

## Step 1: Create a GitHub Repository

1. Go to https://github.com/new
2. Create a new repository:
   - Name: `jpm`
   - Description: "Java Package Manager - CLI tool to add Maven dependencies"
   - Visibility: Public (for open source) or Private
   - Do NOT initialize with README, .gitignore, or LICENSE (we have them)

3. Copy the repository URL (e.g., `https://github.com/your-org/jpm.git` or `git@github.com:your-org/jpm.git`)

## Step 2: Push to GitHub

```bash
cd /Users/user/Documents/Backend/jpm

# Initialize git and add remote
git init
git remote add origin git@github.com:YOUR-ORG/jpm.git
git branch -M main

# Add all files
git add .
git commit -m "Initial JPM MVP commit"

# Push to GitHub
git push -u origin main
```

## Step 3: Verify GitHub Actions is Enabled

1. Go to https://github.com/YOUR-ORG/jpm/settings/actions
2. Confirm "Actions permissions" is set to "Allow all actions and reusable workflows"
3. The CI workflow (`.github/workflows/ci.yml`) will run automatically on:
   - Push to `main` (build only)
   - Pull requests to `main` (build only)
   - Push of tags starting with `v*` (build + create release)

## Step 4: Create Your First Release

When you're ready to release v1.0.0:

```bash
cd /Users/user/Documents/Backend/jpm

# Update pom.xml version from 1.0.0-SNAPSHOT to 1.0.0 (if not already done)
# Update CHANGELOG.md with release notes

git add pom.xml CHANGELOG.md
git commit -m "Prepare release v1.0.0"

# Create a tag
git tag -a v1.0.0 -m "JPM v1.0.0 - Initial release"

# Push commit and tag
git push origin main
git push origin v1.0.0
```

GitHub Actions will automatically:
1. Build the JAR
2. Generate SHA256 checksums
3. Create a GitHub Release at https://github.com/YOUR-ORG/jpm/releases/tag/v1.0.0
4. Upload `jpm-1.0.0.jar` and `jpm-1.0.0.jar.sha256` to the release

## Step 5: Install From Your Release

Users can now install JPM using the installer script:

```bash
curl -sSL https://raw.githubusercontent.com/YOUR-ORG/jpm/main/install-jpm.sh | bash -s -- v1.0.0
```

Or use the latest version:

```bash
curl -sSL https://raw.githubusercontent.com/YOUR-ORG/jpm/main/install-jpm.sh | bash -s -- latest
```

## Step 6: Homebrew Distribution (Optional)

To allow installation via Homebrew:

1. Create a separate repository for your Homebrew tap:
   - Repository name: `homebrew-jpm`
   - URL: `https://github.com/YOUR-ORG/homebrew-jpm`

2. Create a formula file: `Formula/jpm.rb` in that repo with the contents from `homebrew-formula.rb` (see below)

3. Users can then:
   ```bash
   brew tap YOUR-ORG/jpm https://github.com/YOUR-ORG/homebrew-jpm
   brew install YOUR-ORG/jpm/jpm
   ```

### Example Homebrew Formula

Create `Formula/jpm.rb` in your `homebrew-jpm` repo:

```ruby
class Jpm < Formula
  desc "JPM - Java Package Manager CLI"
  homepage "https://github.com/YOUR-ORG/jpm"
  url "https://github.com/YOUR-ORG/jpm/releases/download/v1.0.0/jpm-1.0.0.jar"
  sha256 "PASTE-SHA256-HERE"
  license "MIT"

  depends_on "openjdk@17"

  def install
    libexec.install "jpm-1.0.0.jar"
    (bin/"jpm").write <<~EOS
      #!/bin/bash
      exec java -jar #{libexec}/jpm-1.0.0.jar "$@"
    EOS
  end

  test do
    system "#{bin}/jpm", "--version"
  end
end
```

To get the SHA256:

```bash
curl -sSL https://github.com/YOUR-ORG/jpm/releases/download/v1.0.0/jpm-1.0.0.jar.sha256
```

## Future Releases

For each new release:

1. Update version in `pom.xml` (e.g., `1.0.1` for patch, `1.1.0` for minor)
2. Update `CHANGELOG.md` with release notes
3. Commit and tag:
   ```bash
   git add pom.xml CHANGELOG.md
   git commit -m "Prepare release vX.Y.Z"
   git tag -a vX.Y.Z -m "JPM vX.Y.Z"
   git push origin main
   git push origin vX.Y.Z
   ```

GitHub Actions will handle the rest automatically.

## Continuous Integration

Your CI pipeline now:
- ✓ Runs on every push and PR (builds and tests)
- ✓ Automatically creates releases when you push a tag
- ✓ Generates checksums for verification
- ✓ Uploads all artifacts to GitHub Releases

## File Reference

- `.github/workflows/ci.yml` – GitHub Actions workflow for build and release
- `install-jpm.sh` – Installation script users run
- `bin/jpm` – Local development launcher
- `LICENSE` – MIT License (change if needed)
- `CHANGELOG.md` – Release notes and version history
- `CONTRIBUTING.md` – Guidelines for contributors
- `.gitignore` – Files to exclude from version control
- `README.md` – User-facing documentation

## Quick Checklist for Release Day

- [ ] Update `CHANGELOG.md`
- [ ] Update `pom.xml` version (remove `-SNAPSHOT`)
- [ ] Test locally: `mvn clean package && ./bin/jpm --version`
- [ ] Commit: `git commit -m "Prepare release vX.Y.Z"`
- [ ] Tag: `git tag -a vX.Y.Z -m "JPM vX.Y.Z"`
- [ ] Push: `git push origin main && git push origin vX.Y.Z`
- [ ] Verify release on GitHub: https://github.com/YOUR-ORG/jpm/releases
- [ ] Test installer: `curl -sSL https://raw.githubusercontent.com/YOUR-ORG/jpm/main/install-jpm.sh | bash -s -- vX.Y.Z`
- [ ] Announce release (Twitter, blog, etc.)

Good luck with your JPM project!

