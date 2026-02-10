# YCSB - Custom Build for shvm.in

This is a customized fork of [YCSB (Yahoo! Cloud Serving Benchmark)](https://github.com/brianfrankcooper/YCSB) published under the `in.shvm.ycsb` groupId.

## What's Different

- **GroupId**: `in.shvm.ycsb` (instead of `site.ycsb`)
- **Version**: `0.18.0-shivam-SNAPSHOT`
- **Publishing**: Automated builds to GitHub Packages
- **Custom Bindings**: Includes shvm-db binding for benchmarking

## Automated Publishing

Every push to `main` or `master` triggers:
1. Maven build with tests
2. Automatic deployment to GitHub Packages

## Using This Package

Add to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/shivamd20/YCSB</url>
  </repository>
</repositories>

<dependency>
  <groupId>in.shvm.ycsb</groupId>
  <artifactId>core</artifactId>
  <version>0.18.0-shivam-SNAPSHOT</version>
</dependency>
```

### Authentication

Add to your `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

**Token Requirements**: `read:packages`, `write:packages` (for publishing)

## Building Locally

```bash
mvn clean package -DskipTests
```

## Publishing Manually

```bash
mvn clean deploy -DskipTests
```

## Versioning Strategy

- **SNAPSHOT builds**: Every push to main overwrites the snapshot
- **Release builds**: Tag with `v*` (e.g., `v0.18.0-shivam`) for immutable releases

To create a release:

```bash
# Update version in pom.xml to remove -SNAPSHOT
git tag v0.18.0-shivam
git push origin v0.18.0-shivam
```

## Original YCSB Documentation

For YCSB usage, workloads, and bindings documentation, see the [upstream repository](https://github.com/brianfrankcooper/YCSB).

## License

Apache License 2.0 (same as upstream YCSB)
