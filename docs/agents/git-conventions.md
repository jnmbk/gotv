# Git & Commit Conventions for Agents

Guidelines on how autonomous agents and contributors must manage git commits in this repository.

## Requirements

1. **Conventional Commits**: Every commit message MUST strictly adhere to the [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) specification.
2. **Granular Commits**: Commit changes in small, logical units. Do not bundle multiple unrelated files or distinct logical steps into a single commit.
3. **Commit Incrementally**: When executing an issue or specification:
   - Commit interface/model changes first.
   - Commit implementation details in cohesive chunks.
   - Commit unit/integration tests alongside or immediately following their target code.
   - Commit docs/specs updates separately from functional code changes when applicable.

## Format

```text
<type>(<scope>): <subject>
```

- **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- **Scope**: Lowercase noun indicating the affected subsystem (e.g., `player`, `channel`, `qr`, `storage`, `spec`, `agents`).
- **Subject**: Imperative mood, lowercase start, no trailing period (max 72 characters).

Refer to [CONTRIBUTING.md](../../CONTRIBUTING.md) for full examples and scope definitions.
