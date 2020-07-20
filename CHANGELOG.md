# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.0 - 2020-07-20
### Added
- Add optional dependencies to release.yml

## 1.0.0 - 2020-06-17
### Changed
- Migrate to new packages.scm-manager.org nexus ([#6](https://github.com/scm-manager/smp-maven-plugin/pull/6))

## 1.0.0-rc7 - 2020-04-20
### Added
- Goal to copy plugin.xml and force recompile if necessary ([#3](https://github.com/scm-manager/smp-maven-plugin/pull/3))
- Goal to validate common pitfalls ([#4](https://github.com/scm-manager/smp-maven-plugin/pull/4))
- Goal to fix common pitfalls ([#4](https://github.com/scm-manager/smp-maven-plugin/pull/4))

### Removed
- Goal to fix package.json version ([#4](https://github.com/scm-manager/smp-maven-plugin/pull/4))

### Changed
- Resolve versions for buildfrontend maven plugin, nodejs and yarn from plugin pom ([#5](https://github.com/scm-manager/smp-maven-plugin/pull/5))

## 1.0.0-rc6 - 2020-04-14
### Added
- Goal to write release descriptor
- Goal to adjust version in package json

## 1.0.0-rc5 - 2020-03-31

### Changed
- Store version of plugin dependencies ([#2](https://github.com/scm-manager/smp-maven-plugin/pull/2))

## 1.0.0-rc4 - 2020-02-19

### Changed

- Update nodejs to version 12.16.1
- Update yarn to version 1.22.0
- Updated Jetty from 9.2.7.v20150116 to 9.4.26.v20200117 ([#1](https://github.com/scm-manager/smp-maven-plugin/pull/1))

## 1.0.0-rc3 - 2020-02-03

### Added

- JAXB dependencies in order to fix builds on Java versions > 8

## 1.0.0-rc2 - 2020-01-13

### Added

- Support for optional plugin dependencies
