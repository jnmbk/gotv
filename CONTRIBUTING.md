# Contributing to GoTV

Thank you for your interest in contributing to GoTV! To maintain code quality, clarity, and an easily navigable git history, all contributors and agents must follow these guidelines.

---

## Git & Commit Guidelines

GoTV strictly adheres to **[Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/)** and requires **granular, atomic commits**.

### 1. Granular Commits

- **Atomic changes**: Each commit must represent a single, focused logical change.
- **Do not bundle unrelated work**: Never combine refactoring, feature work, dependency updates, and documentation into a single monolithic commit.
- **Build and test safety**: Each commit should ideally leave the repository in a compilable, passing state.
- **Step-by-step progression**: When working on tickets or complex tasks, commit at logical intermediate milestones (e.g. models first, then data layer, then UI seam, then tests) rather than one massive commit at the end.

---

### 2. Commit Message Structure

```text
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

#### Examples

- `feat(player): add lean ExoPlayer pipeline with constrained buffers`
- `fix(scanner): handle camera permission denial gracefully`
- `docs(spec): document Conventional Commits standard in architecture spec`
- `refactor(storage): separate logo cache eviction from channel dao`
- `test(channel): add Robolectric test for channel grid empty state`
- `chore(deps): update AndroidX Media3 dependencies to 1.3.1`

---

### 3. Allowed Types

| Type | Description |
| :--- | :--- |
| `feat` | A new feature or capability for the user or system |
| `fix` | A bug fix or error correction |
| `docs` | Documentation updates, specifications, or markdown notes |
| `style` | Formatting, whitespace, semicolon fixes (no code logic changes) |
| `refactor` | Code restructuring that neither fixes a bug nor adds a feature |
| `perf` | Code changes that improve performance or reduce memory/RAM footprint |
| `test` | Adding missing tests or correcting existing tests |
| `build` | Changes that affect the build system or external dependencies (Gradle, plugins) |
| `ci` | Changes to CI configuration files and scripts |
| `chore` | Routine tasks, maintenance, tool configurations, repo hygiene |
| `revert` | Reverts a previous commit |

---

### 4. Recommended Scopes

Scopes help contextualize where changes occur. Common scopes include:

- `player`: Video playback, ExoPlayer configuration, stream buffering, audio controls.
- `channel`: Channel data models, channel grid dashboard, channel list sidebar.
- `qr`: QR code scanning (CameraX, ML Kit), image import.
- `storage`: Database persistence, file caching, logo downsampling.
- `ui`: Shared UI themes, compose components, navigation.
- `spec`: Specifications, issue definitions, agent task files.
- `agents`: Agent configurations, skills, and prompts.

---

### 5. Formatting Rules

1. **Imperative mood**: Use "add", "fix", "change", not "added", "fixes", "changing".
2. **Summary length**: Keep the header line under 72 characters.
3. **No trailing period**: Do not end the description line with a period.
4. **Body explanations**: Use the optional body to explain the *what* and *why*, especially for memory-sensitive optimizations or architectural choices.
5. **Breaking changes**: Append a `!` before the colon (`feat(player)!: ...`) or specify `BREAKING CHANGE: <explanation>` in the footer.
