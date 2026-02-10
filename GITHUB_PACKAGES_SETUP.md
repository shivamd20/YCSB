# GitHub Packages Publishing Setup - Complete Guide

## ✅ What Has Been Done

### 1. Maven Coordinates Updated
- **GroupId**: Changed from `site.ycsb` to `in.shvm.ycsb`
- **Version**: Changed from `0.18.0-SNAPSHOT` to `0.18.0-shivam-SNAPSHOT`
- **Scope**: Updated in all 50+ pom.xml files across the project

### 2. Distribution Management Configured
Updated root `pom.xml` with GitHub Packages repository:
```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/shivamd20/YCSB</url>
  </repository>
</distributionManagement>
```

### 3. GitHub Actions Workflow Created
File: `.github/workflows/build-and-publish.yml`

**Triggers:**
- Push to `main` or `master` branch
- Git tags matching `v*` pattern

**Actions:**
- Checkout code
- Set up JDK 11 (required for YCSB)
- Configure Maven with GitHub token authentication
- Build project (skip tests for speed)
- Deploy to GitHub Packages

**Permissions:**
- `contents: read` - Read repository code
- `packages: write` - Publish to GitHub Packages

## 🚀 Next Steps

### Step 1: Push to GitHub
```bash
cd /Users/shivam.dwivedi/Documents/personal\ projects/shvm-db-benchmark/ycsb
git add .
git commit -m "Configure GitHub Packages publishing with in.shvm.ycsb groupId"
git push origin main
```

### Step 2: Verify GitHub Actions
1. Go to: https://github.com/shivamd20/YCSB/actions
2. Watch the "Build and Publish YCSB" workflow
3. Ensure it completes successfully
4. Check the "Packages" tab on your repository

### Step 3: Verify Package Published
After successful workflow run:
1. Visit: https://github.com/shivamd20/YCSB/packages
2. You should see packages like:
   - `in.shvm.ycsb/core`
   - `in.shvm.ycsb/shvmdb-binding`
   - etc.

## 📦 Using the Published Package

### In Other Projects

Add to `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/shivamd20/YCSB</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>in.shvm.ycsb</groupId>
    <artifactId>core</artifactId>
    <version>0.18.0-shivam-SNAPSHOT</version>
  </dependency>
  
  <!-- For shvm-db binding -->
  <dependency>
    <groupId>in.shvm.ycsb</groupId>
    <artifactId>shvmdb-binding</artifactId>
    <version>0.18.0-shivam-SNAPSHOT</version>
  </dependency>
</dependencies>
```

### Authentication Setup

Create/update `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

**GitHub Token Requirements:**
- Scope: `read:packages` (minimum for consuming)
- Scope: `write:packages` (for publishing)
- Scope: `repo` (if repository is private)

**Create token at:** https://github.com/settings/tokens

## 🏷️ Versioning Strategy

### SNAPSHOT Builds (Current)
- **Version**: `0.18.0-shivam-SNAPSHOT`
- **Behavior**: Every push to main overwrites the snapshot
- **Use case**: Active development, latest changes

### Release Builds (Future)
To create an immutable release:

1. **Update version in pom.xml:**
   ```bash
   # Change version from 0.18.0-shivam-SNAPSHOT to 0.18.0-shivam
   python3 update_poms.py  # After modifying the script
   ```

2. **Commit and tag:**
   ```bash
   git add .
   git commit -m "Release v0.18.0-shivam"
   git tag v0.18.0-shivam
   git push origin main
   git push origin v0.18.0-shivam
   ```

3. **GitHub Actions will:**
   - Build the tagged version
   - Publish immutable artifact to GitHub Packages

## 🔧 Manual Publishing (Optional)

If you need to publish manually:

```bash
# Ensure you have GitHub token in ~/.m2/settings.xml
mvn clean deploy -DskipTests
```

## 📝 Package Naming Convention

All packages will be published under:
- **Domain**: `in.shvm.ycsb`
- **Rationale**: Uses your domain `shvm.in` in reverse DNS notation
- **Benefits**: 
  - No collision with upstream `site.ycsb`
  - Clear ownership and branding
  - Professional package naming

## 🎯 What Gets Published

The following artifacts will be published to GitHub Packages:
- `in.shvm.ycsb:core` - YCSB core library
- `in.shvm.ycsb:binding-parent` - Parent for bindings
- `in.shvm.ycsb:shvmdb-binding` - Your custom shvm-db binding
- All other database bindings (50+ modules)

## 🔍 Troubleshooting

### Build Fails
- Check Java version (needs JDK 11)
- Verify all pom.xml files updated correctly
- Check GitHub Actions logs

### Authentication Fails
- Verify GitHub token has correct scopes
- Check `~/.m2/settings.xml` configuration
- Ensure server `id` matches `github` in pom.xml

### Package Not Visible
- Check repository permissions
- Verify workflow completed successfully
- GitHub Packages may take a few minutes to index

## 📚 Additional Resources

- **GitHub Packages Maven Docs**: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry
- **YCSB Documentation**: https://github.com/brianfrankcooper/YCSB/wiki
- **Custom Build Notes**: See `CUSTOM_BUILD.md`

---

**Status**: ✅ Setup Complete - Ready to push and publish
**Next Action**: Push to GitHub and verify workflow execution
