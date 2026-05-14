# Release Process

This document describes how to release `benatti-auth-starter` to Maven Central and `@benatti/ng-auth-lib` to npm.

---

## Required One-Time Setup

### GPG Key (for Maven Central signing)

```bash
# Generate key
gpg --gen-key

# List keys and note the key ID (8-char suffix)
gpg --list-secret-keys --keyid-format SHORT

# Upload public key to a keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# Export private key as base64 (for GitHub Secret)
gpg --export-secret-keys <KEY_ID> | base64
```

### Sonatype Central Portal account

1. Register at [central.sonatype.com](https://central.sonatype.com)
2. Claim namespace `io.github.benatti-dev` (auto-verified via GitHub OAuth)
3. Generate a **User Token** (Account → User Token)

### npm account

1. Register at [npmjs.com](https://www.npmjs.com)
2. Create an **Automation token** (Account → Access Tokens → Automation)

### GitHub Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|--------|-------|
| `MAVEN_CENTRAL_USERNAME` | Sonatype User Token username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype User Token password |
| `GPG_PRIVATE_KEY` | base64-encoded private GPG key (see above) |
| `GPG_PASSPHRASE` | GPG key passphrase |
| `NPM_TOKEN` | npm Automation token |

---

## Java Library — `benatti-auth-starter`

### CI (every push / PR)

Runs automatically via [`.github/workflows/java-ci.yml`](.github/workflows/java-ci.yml):
- compiles, runs all 41 unit tests
- uploads Surefire test results as artifact

### Release

**Step 1 — update version**

```bash
cd benatti-auth-starter
# bump version in pom.xml, e.g. 0.0.1 → 1.0.0
mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false
```

**Step 2 — run full test suite locally**

```bash
mvn verify
```

**Step 3 — update CHANGELOG.md**, move `[Unreleased]` entries under the new version heading.

**Step 4 — commit, tag, push**

```bash
git add benatti-auth-starter/pom.xml CHANGELOG.md
git commit -m "chore: release benatti-auth-starter v1.0.0"
git tag v1.0.0
git push origin main --tags
```

**Step 5 — GitHub Actions publishes automatically**

The [`.github/workflows/java-publish.yml`](.github/workflows/java-publish.yml) workflow triggers on the `v*` tag:
1. Runs `mvn verify`
2. Runs `mvn deploy -P release` — signs artifacts with GPG, uploads to Maven Central
3. Creates a GitHub Release with auto-generated release notes

**Step 6 — verify on Maven Central**

> Maven Central sync takes up to 30 minutes.

```xml
<dependency>
  <groupId>io.github.benatti-dev</groupId>
  <artifactId>benatti-auth-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Step 7 — bump to next development version**

```bash
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT -DgenerateBackupPoms=false
git add benatti-auth-starter/pom.xml
git commit -m "chore: begin 1.1.0-SNAPSHOT"
git push origin main
```

---

## Angular Library — `@benatti/ng-auth-lib`

### CI (every push / PR)

Runs automatically via [`.github/workflows/angular-ci.yml`](.github/workflows/angular-ci.yml):
- installs dependencies (`npm ci`)
- runs all 55 Jest tests with coverage
- builds the library via `ng-packagr` (smoke test)

### Release

**Step 1 — update version**

```bash
cd ng-auth-lib
npm version 1.0.0 --no-git-tag-version
```

**Step 2 — run tests locally**

```bash
npm test
npm run build          # verify ng-packagr build succeeds
```

**Step 3 — update CHANGELOG.md**.

**Step 4 — commit, tag, push**

```bash
git add ng-auth-lib/package.json CHANGELOG.md
git commit -m "chore: release @benatti/ng-auth-lib v1.0.0"
git tag ng-v1.0.0
git push origin main --tags
```

**Step 5 — GitHub Actions publishes automatically**

The [`.github/workflows/angular-publish.yml`](.github/workflows/angular-publish.yml) workflow triggers on the `ng-v*` tag:
1. Runs `npm test`
2. Builds the library with `ng-packagr`
3. Runs `npm publish --access public` from `dist/ng-auth-lib/`
4. Creates a GitHub Release

**Step 6 — verify on npm**

```bash
npm info @benatti/ng-auth-lib
```

```json
// package.json of a consumer project
"dependencies": {
  "@benatti/ng-auth-lib": "^1.0.0"
}
```

**Step 7 — bump to next development version**

```bash
npm version 1.1.0-beta.0 --no-git-tag-version
git add ng-auth-lib/package.json
git commit -m "chore: begin 1.1.0-beta.0"
git push origin main
```

---

## Release Checklist

### Before tagging

- [ ] All tests pass locally (`mvn verify` / `npm test`)
- [ ] Version updated in `pom.xml` / `package.json`
- [ ] `CHANGELOG.md` updated — `[Unreleased]` moved to new version section
- [ ] All open PRs merged
- [ ] README examples reference the new version

### After release

- [ ] Maven Central shows the artifact (allow 30 min sync)
- [ ] npm registry shows the package (`npm info @benatti/ng-auth-lib`)
- [ ] GitHub Release created with correct tag and release notes
- [ ] Announce release (GitHub Discussions, README badge, etc.)
- [ ] Bump to next development version

---

## Version Strategy

This project uses [Semantic Versioning](https://semver.org):

| Change type | Example | When to use |
|-------------|---------|-------------|
| **PATCH** `x.x.Z` | `1.0.1` | Bug fix, no API change |
| **MINOR** `x.Y.0` | `1.1.0` | New backward-compatible feature |
| **MAJOR** `X.0.0` | `2.0.0` | Breaking API change |

Git tag convention:
- Java releases: `v1.0.0`
- Angular releases: `ng-v1.0.0`
