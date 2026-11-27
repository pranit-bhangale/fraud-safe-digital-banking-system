# Usage: make help
ENV ?= dev
PLAY = ansible-playbook bootstrap.yml -e env_name=$(ENV)

.PHONY: help check setup down destroy lint

help:
	@echo "make check                     Dry run (shows what would change)"
	@echo "make setup                     Apply the full bootstrap"
	@echo "make setup TAGS=docker,health  Run only some stages"
	@echo "make down                      Stop all stacks (keep data)"
	@echo "make destroy                   Stop all stacks and delete volumes"
	@echo "make lint                      Syntax check"
	@echo "Use ENV=prod to load project.prod.yml on top of project.yml"

check:
	$(PLAY) --check --diff

setup:
	$(PLAY) $(if $(TAGS),--tags $(TAGS),)

down:
	ansible-playbook teardown.yml

destroy:
	ansible-playbook teardown.yml -e remove_volumes=true

lint:
	ansible-playbook bootstrap.yml --syntax-check
	ansible-playbook teardown.yml --syntax-check
