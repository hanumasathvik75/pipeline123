# Contributing to IgnitionAI

Thank you for working on IgnitionAI! This guide will walk you through the full development workflow.

---

## 🔧 One-Time Setup

### 1. Accept the Repository Invitation
Shashank will invite you as a **Collaborator** on GitHub. Accept the email invite before cloning.

### 2. Clone and Configure

```bash
git clone https://github.com/shankscodz/IgnitionAI.git
cd IgnitionAI
```

### 3. Set Up the Desktop App

```bash
cd desktop
pip install -r requirements.txt
cp app/config.example.py app/config.py
# Edit config.py and add your Gemini API key
python main.py   # Verify it launches
```

### 4. Configure Git Identity

```bash
git config user.name "Your Name"
git config user.email "your@email.com"
```

---

## 🌿 Daily Workflow

### Start of Day — Get Latest Code

```bash
git checkout dev
git pull origin dev
git checkout -b feature/<your-name>/<what-youre-building>
```

### During Development — Commit Often

```bash
git add .
git commit -m "feat(engine): add Bayesian prior update logic"
```

**Commit types:**
- `feat` — New feature
- `fix` — Bug fix
- `docs` — Documentation only
- `refactor` — Code restructure, no behavior change
- `test` — Adding tests
- `chore` — Tooling, configs, CI

### End of Day — Push Your Branch

```bash
git push origin feature/<your-name>/<what-youre-building>
```

---

## 🔁 Opening a Pull Request

1. Go to **github.com/shankscodz/IgnitionAI**
2. Click **"Compare & pull request"** on your branch
3. Set base branch to **`dev`** (NOT `main`)
4. Fill out the PR template
5. Tag **@shankscodz** for review
6. Address any review comments
7. Shashank will merge when approved ✅

---

## 🚫 Rules — Never Break These

| Rule | Why |
|------|-----|
| Never push directly to `main` | Protected branch — must go through PR |
| Never push directly to `dev` | Always work on a `feature/` branch |
| Never commit `config.py` | Contains API keys — use `config.example.py` |
| Never commit `*.apk` or `*.aab` | Binary files bloat the repo |
| Always `git pull` before starting new work | Avoids merge conflicts |

---

## 🗂️ Module Ownership

| Module | Owner |
|--------|-------|
| `desktop/app/engine/` | Intern 2 (ML/Data) |
| `desktop/app/ui/` | Intern 1 (Frontend) |
| `desktop/app/data/simulator.py` | Intern 1 or 2 |
| `mobile/` | Intern 1 (Frontend) |
| `backend/` | Intern 2 (ML/Data) |

---

## 💬 Communication

- Daily standups on WhatsApp/Discord: "What I did / What I'm doing / Any blockers"
- Open a GitHub Issue for bugs or feature requests
- Tag Shashank on PRs for review

---

## ❓ Common Issues

**App doesn't launch:**
```bash
pip install -r requirements.txt  # Re-install deps
```

**Gemini AI not working:**
- Check your API key is in `desktop/app/config.py` (not config.example.py)
- Make sure you have internet access

**Merge conflicts:**
```bash
git checkout dev
git pull origin dev
git checkout your-branch
git rebase dev   # Resolve conflicts in VS Code/editor
git push --force-with-lease origin your-branch
```
