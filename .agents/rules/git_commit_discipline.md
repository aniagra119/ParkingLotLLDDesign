# Git Commit & Push Discipline Rule

1. **Meaningful Commits**: Every logically complete milestone, phase, refactoring step, or architectural update must be committed with a clean, conventional commit message (e.g. `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`, `test: ...`).
2. **Always Push**: Immediately push commits to the remote repository (`git push origin <branch>`) so that the project history remains consistent, synchronized, and durable.
3. **No Dangling Uncommitted Code**: Keep the git status clean; never leave significant working state unstaged or uncommitted after completing a unit of work.
