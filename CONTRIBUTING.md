# Contributing

The team uses `main` for stable work, `dev` for integration, and focused feature or documentation branches for individual changes. Open a pull request into `dev` first and obtain at least one approval before merging. Changes promoted from `dev` to `main` should also use a pull request.

## Stage changes deliberately

Review the working tree and stage only the files intended for the commit:

```powershell
git status
git add path\to\file
git diff --cached
```

Do not use the commit helper to stage files. It never changes branches, stages files, pushes commits, or changes Git configuration. It also refuses to commit common secret and signing-file formats, including Firebase configuration files, environment files, keystores, private keys, and service-account JSON files.

## Create a Conventional Commit

From the repository root, run:

```powershell
.\scripts\commit.ps1
```

If local execution policy prevents the script from starting, use a process-only override rather than changing the machine or user policy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\commit.ps1
```

The helper displays the current branch, working tree status, and staged diff. It then asks for a required type, optional scope, required short summary, optional longer description, and confirmation. Only the already-staged changes are committed.

Supported types:

| Type | Use for |
| --- | --- |
| `feat` | New functionality |
| `fix` | Bug fixes |
| `docs` | Documentation |
| `refactor` | Code improvements without behavior changes |
| `test` | Tests |
| `style` | Formatting-only changes |
| `chore` | Maintenance |
| `build` | Gradle and build configuration |
| `ci` | GitHub Actions and CI configuration |
| `perf` | Performance improvements |
| `revert` | Reverting a previous change |

Examples:

```text
feat: implement expense tracking screen
fix(auth): resolve login validation issue
docs: update Firestore setup instructions
build: configure Kotlin dependencies
test(repository): add transaction repository tests
```

The required first line follows this format:

```text
<type>(optional-scope): <required-summary>
```

Scopes are optional and lowercase. The short summary is mandatory and must contain a letter or number and at least five trimmed characters. When prompted with `Add a longer description? (y/N)`, press Enter to skip it. To add one, enter `Y`, type one or more lines, and finish with an empty line:

```text
feat: implement expense tracking

Add expense categories and input validation.
Update the transaction repository.
```

## Run Android verification

The project uses the checked-in Gradle Wrapper. Run the available checks together with:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --console=plain
```

Or ask the commit helper to run the same checks before prompting for the commit:

```powershell
.\scripts\commit.ps1 -Verify
```

The commit is not created if a build, Android lint, or unit-test task fails. The project does not currently configure ktlint, Detekt, Spotless, or another Kotlin formatting task, so no formatting command is included.

## Pull request protection

The `Commit message validation` GitHub Actions workflow runs for pull requests targeting `dev` or `main`, including updates and force-pushes. It validates every non-merge commit using the same PowerShell rules as the local helper and reports each invalid commit.

The repository currently uses merge commits, so generated merge commits are excluded and PR titles are not enforced. If the team changes to squash merging, add PR-title validation because the PR title normally becomes the squash commit subject.

Configure branch protection or repository rulesets for both `dev` and `main` to require:

- a pull request;
- at least one approving review; and
- the `Validate commits` status check.

To repair a rejected latest commit, amend its message and push the rewritten commit safely:

```powershell
git commit --amend -m "fix(auth): resolve login validation issue"
git push --force-with-lease
```

For older commits, use interactive rebase with `reword`, then push with `--force-with-lease`. Coordinate rewritten shared history with the other team members first.
