# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| Latest  | ✅        |

## Reporting a Vulnerability

Please **do not** report security vulnerabilities through public GitHub issues.

Instead, use GitHub's private vulnerability reporting:
[Report a vulnerability](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle/security/advisories/new)

You can expect an initial response within **5 business days**.
Once the issue is confirmed, a patch will be released as soon as possible, and you will be credited in the release notes unless you prefer to remain anonymous.

## Security Practices

- All dependencies are monitored for CVEs via Dependabot and a custom NVD-based security scan integrated into the CI/CD pipeline.
- Secrets are never stored in the repository. All credentials are managed via GitHub Secrets and injected at runtime.
- GitHub Actions workflows are pinned to commit SHAs to prevent supply-chain attacks.
