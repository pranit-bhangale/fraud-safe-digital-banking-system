# project-bootstrap

A reusable Ansible project that sets up any new project from one config file.
You edit `project.yml`; the playbook and roles never change.

## Quick start

```bash
# 1. Install Ansible (Windows: do this inside WSL2, Ubuntu)
sudo apt update && sudo apt install -y ansible git

# 2. Copy this folder into (or next to) your new project, then edit the config
cp project.yml project.yml.bak      # optional
nano project.yml

# 3. Preview, then apply
make check          # dry run with diff
make setup          # apply everything
```

Try the working example first:

```bash
ansible-playbook bootstrap.yml -e @examples/fraud-safe-banking/project.yml --check --diff
```

## What it does (in order)

| Stage | Tag | Role | What it handles |
|---|---|---|---|
| 1 | `prereq` | prerequisites | Verifies tools (git, docker, java...) and fails with your hint |
| 2 | `commands`, `pre` | commands | Commands with `phase: pre` |
| 3 | `scaffold` | scaffold | Directories, copied files, Jinja templates, symlinks, downloads |
| 4 | `env` | secrets_env | Generates stable secrets, writes `.env` files |
| 5 | `git` | git_repo | `git init`, `.gitignore`, optional first commit, clones |
| 6 | `docker` | docker_stack | `docker compose up -d` for each stack |
| 7 | `commands`, `post` | commands | Commands with `phase: post` (default) |
| 8 | `health` | health_checks | Waits for ports, HTTP endpoints, container health |

Run only some stages: `make setup TAGS=docker,health` or
`ansible-playbook bootstrap.yml --tags scaffold,env`.

## Layout

```
project-bootstrap/
├── project.yml          # EDIT THIS per project
├── bootstrap.yml        # generic playbook
├── teardown.yml         # docker compose down
├── ansible.cfg, inventory.ini, Makefile
├── files/               # your static files to copy
├── templates/           # Jinja2 templates (gitignore.j2 included)
├── roles/               # one role per stage
├── examples/            # filled-in configs
└── .secrets/            # generated on first run (gitignored)
```

## Config reference (project.yml)

| Key | Purpose |
|---|---|
| `project_name`, `project_root`, `env_name` | Identity, target folder, overlay selector |
| `features` | Switch stages on/off |
| `required_tools` | `{name, check?, hint?}` |
| `directories` | Path string or `{path, mode}` |
| `managed_files` | `{src, dest, mode?, backup?, overwrite?}` |
| `rendered_files` | Same, but rendered as Jinja2 |
| `symlinks`, `downloads` | Links and `get_url` downloads |
| `secrets` | `{name, length?}` available as `secrets_map.<name>` |
| `env_files` | `{dest, vars, mode?, overwrite?}` (default mode 0600, no overwrite) |
| `git_settings`, `git_repos`, `gitignore_extra` | Repo init and clones |
| `docker_stacks` | `{name, compose_file, project_dir?, env_file?, profiles?, build?, pull?, wait?}` |
| `setup_commands` | `{name, cmd, phase?, chdir?, creates?, env?, retries?, delay?, ignore_errors?, show_output?, enabled?}` |
| `health_checks` | `ports`, `http`, `containers` |
| `endpoints` | Lines printed at the end |

## Common workflows

**New project**
1. Copy this folder in, edit `project.yml`, put files in `files/`.
2. `make check`, then `make setup`.

**Different environments.** Create `project.prod.yml` containing only the keys to change,
then run `make setup ENV=prod`. Overlay keys replace the same keys from `project.yml`
(lists are replaced, not merged), so repeat the whole list when overriding one.

**Secrets.** Declare them under `secrets`, reference them as `{{ secrets_map.NAME }}` in
`env_files` or templates. They are created once in `.secrets/` and reused on every run.
Back that folder up or regenerate with care: changing a database password in `.env`
does not change it inside an already-initialised database volume.

**Tear down.** `make down` stops the stacks. `make destroy` also deletes their volumes.

**Add a stage.** Create `roles/<name>/tasks/main.yml` plus `defaults/main.yml`, then add an
`import_role` task with a tag in `bootstrap.yml`.

## Notes and gotchas

- **Windows:** Ansible's control node needs WSL2. Enable Docker Desktop's WSL integration.
  Projects on `/mnt/c` work but are slower and ignore Linux file modes, so prefer `~/projects`.
- **`wait: true`** uses `docker compose --wait`. Leave it `false` if a stack has one-shot
  containers (like an init job that exits); use `health_checks` instead.
- **Idempotent:** re-running is safe. Existing differing files are backed up
  (`name.<timestamp>~`) before being replaced; set `overwrite: false` to never touch one.
- **Commands use bash** (`/bin/bash`), so pipes and `&&` work. Use `creates:` to make a
  command run only once.
- **Check mode** (`--check`) skips shell commands and docker, so it only previews file and
  directory changes.
- **Requires** ansible-core 2.13+ and no extra collections.
