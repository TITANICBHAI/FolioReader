# Replit setup

## Push to GitHub

The repository includes a manual `Push to GitHub` workflow. It runs
`scripts/push-to-github.sh`, which:

- stages repository changes;
- creates a commit using the first command-line argument, or
  `PUSH_COMMIT_MESSAGE`, when changes are present; and
- pushes the current branch to the `origin` GitHub remote.

The script reads `GITHUB_PERSONAL_ACCESS_TOKEN` from Replit Secrets through the
process environment. It does not write the token to Git configuration, the
remote URL, or command output.

The workflow is intentionally not configured to auto-start. Run it manually
after reviewing the staged changes. A new import from GitHub recreates the
workflow because its definition is stored in `.replit`.

## Package rename automation

The Android package is now `com.tbtechs.folioreader`, with the app module
application ID and source paths aligned to that package. To repeat the
migration on a matching imported copy, run:

```bash
bash scripts/rename-package.sh
```

The script updates package references and moves the Kotlin source and test
directories. Product-facing class, screen, and export names use `Folio Reader`;
persisted `lexiread` keys remain unchanged for backward compatibility.