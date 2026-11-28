ENV ?= dev
PLAY = ansible-playbook bootstrap.yml -e env_name=$(ENV)

.PHONY: help check setup setup-client down status lint

help:
	@echo "Usage:"
	@echo "  make check                     Dry run (shows what would change)"
	@echo "  make setup                     Apply the full bootstrap (start Java services sequentially)"
	@echo "  make setup-client              Apply using the client configuration (my-cloud-platform)"
	@echo "  make setup TAGS=database       Provision & verify databases only"
	@echo "  make setup TAGS=services       Start only local services"
	@echo "  make down                      Stop all services in reverse order (3 -> 2 -> 1)"
	@echo "  make status                    Show status of managed service processes (PIDs/logs)"
	@echo "  make lint                      Syntax check playbooks"
	@echo "  Use ENV=prod to load project.prod.yml on top of project.yml"

check:
	$(PLAY) --check --diff

setup:
	$(PLAY) $(if $(TAGS),--tags $(TAGS),)

setup-client:
	ansible-playbook bootstrap.yml -e @project.client.yml

down:
	ansible-playbook teardown.yml

status:
	@echo "=== Local Process Status ==="
	@if [ -d .pids ]; then for f in .pids/*.pid; do [ -e "$$f" ] || continue; name=$$(basename "$$f" .pid); pid=$$(cat "$$f"); if kill -0 "$$pid" 2>/dev/null; then echo "  [RUNNING] $$name (PID $$pid)"; else echo "  [STOPPED] $$name (stale PID $$pid)"; fi; done; else echo "  No .pids directory found."; fi

lint:
	ansible-playbook bootstrap.yml --syntax-check
	ansible-playbook teardown.yml --syntax-check
