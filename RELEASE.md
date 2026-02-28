# Release Guide

This document explains how to release new versions of AiTrailer SDK.

## Versioning Strategy

We follow [Semantic Versioning](https://semver.org/):
- **MAJOR** (X.0.0): Breaking API changes
- **MINOR** (0.X.0): New features, backward-compatible
- **PATCH** (0.0.X): Bug fixes, backward-compatible

## Release Process

### 1. Update Version

Edit `app/build.gradle.kts` and update the version:

```kotlin
version = "3.9.0"  // Update this
```

### 2. Update Changelog

Add release notes to the GitHub release description.

### 3. Commit Changes

```bash
git add .
git commit -m "chore: bump version to 3.9.0"
git push origin main
```

### 4. Create and Push Tag

```bash
git tag v3.9.0
git push origin v3.9.0
```

### 5. CI/CD Automation

Once the tag is pushed:
1. GitHub Actions will automatically:
   - Build the library
   - Generate sources and javadoc jars
   - Create a GitHub Release
   - Trigger JitPack build

### 6. Verify on JitPack

Visit https://jitpack.io/#Qkprahlad101/AiTrailerSDK to verify the build status.

Users can now use the new version:

```kotlin
implementation("com.github.Qkprahlad101:aitrailer-sdk:3.9.0")
```

## Troubleshooting

### JitPack Build Fails

1. Check the build log at https://jitpack.io/#Qkprahlad101/AiTrailerSDK
2. Common issues:
   - Missing Android SDK components
   - Gradle version incompatibility
   - Missing dependencies

### GitHub Release Not Created

1. Check the Actions tab in GitHub
2. Ensure `GITHUB_TOKEN` permissions are correct (should be automatic)

## Quick Reference Commands

```bash
# Create annotated tag
git tag -a v3.9.0 -m "Release version 3.9.0"

# Push specific tag
git push origin v3.9.0

# Push all tags
git push --tags

# Delete local tag
git tag -d v3.9.0

# Delete remote tag
git push origin --delete v3.9.0
```
