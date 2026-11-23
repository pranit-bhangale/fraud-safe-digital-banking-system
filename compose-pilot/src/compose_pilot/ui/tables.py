"""Formatted Rich table renderers for status, health, endpoints, and configuration."""

from rich.table import Table

from compose_pilot.models import ServiceHealth, ServiceMetadata, ServiceState
from compose_pilot.ui.console import PilotConsole


def render_health_table(console: PilotConsole, health_map: dict[str, ServiceHealth]) -> None:
    """Render formatted service health status table."""
    table = Table(
        title="Service Health & Execution Status",
        header_style="bold cyan",
        show_edge=True,
        show_header=True,
    )
    table.add_column("Service", style="bold", min_width=18)
    table.add_column("Container Status", justify="center")
    table.add_column("Health / Outcome", justify="left")
    table.add_column("Exit Code", justify="right")

    for svc, h in health_map.items():
        state_text = ""
        if h.state == ServiceState.HEALTHY:
            state_text = f"[bold green]{console.symbols.CHECK} Healthy[/bold green]"
        elif h.state == ServiceState.RUNNING:
            state_text = f"[bold green]{console.symbols.CHECK} Running[/bold green]"
        elif h.state == ServiceState.COMPLETED:
            state_text = f"[bold green]{console.symbols.CHECK} Completed (Exit Code 0)[/bold green]"
        elif h.state == ServiceState.STARTING:
            state_text = f"[bold yellow]{console.symbols.WAIT} Starting / Pending[/bold yellow]"
        elif h.state == ServiceState.UNHEALTHY:
            state_text = f"[bold red]{console.symbols.CROSS} Unhealthy[/bold red]"
        elif h.state == ServiceState.FAILED:
            state_text = f"[bold red]{console.symbols.CROSS} Failed (Exit Code {h.exit_code})[/bold red]"
        elif h.state == ServiceState.NOT_FOUND:
            state_text = f"[bold red]{console.symbols.CROSS} Container Not Found[/bold red]"
        else:
            state_text = f"[bold red]{console.symbols.CROSS} Unknown Status ({h.container_status})[/bold red]"

        exit_code_str = str(h.exit_code) if h.exit_code != 0 else "-"
        table.add_row(svc, h.container_status, state_text, exit_code_str)

    console.print(table)


def render_endpoints_table(
    console: PilotConsole, project_name: str, endpoints: list[tuple[str, list[str]]]
) -> None:
    """Render service endpoints and port mappings."""
    console.major_section(f"🌐 {project_name} Services & Endpoints")
    for svc, urls in endpoints:
        for url in urls:
            if url.startswith("https://"):
                console.print(f"  {svc:<18} : [bold green]{url}[/bold green]")
            elif url.startswith("http://"):
                console.print(f"  {svc:<18} : [bold cyan]{url}[/bold cyan]")
            else:
                console.print(f"  {svc:<18} : [dim]{url}[/dim]")


def render_config_table(console: PilotConsole, metadata: dict[str, ServiceMetadata]) -> None:
    """Render resolved service configuration inspection table."""
    table = Table(
        title="Resolved Services & Configuration",
        header_style="bold cyan",
        show_edge=True,
    )
    table.add_column("Service", style="bold", min_width=16)
    table.add_column("Buildable", justify="center")
    table.add_column("Image", style="dim", min_width=25)
    table.add_column("Ports", justify="center")
    table.add_column("Context / Dockerfile", justify="left")

    for svc, meta in metadata.items():
        is_build = "[green]Yes[/green]" if meta.is_buildable else "[dim]No[/dim]"
        ports_str = ", ".join(meta.ports) if meta.ports else "none"
        ctx_str = f"{meta.context} ({meta.dockerfile})" if meta.is_buildable else "-"
        table.add_row(svc, is_build, meta.image, ports_str, ctx_str)

    console.print(table)
