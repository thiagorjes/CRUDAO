"""
SSPDD platform file generator.
Usage: python generate_platform.py --platform claude|cursor|copilot|opencode|all
                                   --path ./workspace --source .agents/
"""

import argparse
import re
import sys
from pathlib import Path


def parse_frontmatter(content: str) -> dict:
    m = re.search(r"^---\n(.*?)\n---", content, re.DOTALL)
    if not m:
        return {}
    fm = {}
    for line in m.group(1).splitlines():
        if ":" in line:
            key, _, val = line.partition(":")
            fm[key.strip()] = val.strip()
    return fm


def strip_frontmatter(content: str) -> str:
    m = re.search(r"^---\n.*?\n---\n", content, re.DOTALL)
    if m:
        return content[m.end() :]
    return content


def discover_skills(source: Path) -> list[dict]:
    skills = []
    skills_dir = source / "skills"
    if not skills_dir.exists():
        return skills
    for skill_dir in sorted(skills_dir.iterdir()):
        if not skill_dir.is_dir():
            continue
        skill_md = skill_dir / "SKILL.md"
        if not skill_md.exists():
            continue
        content = skill_md.read_text(encoding="utf-8")
        fm = parse_frontmatter(content)
        skills.append(
            {
                "name": fm.get("name", skill_dir.name),
                "description": fm.get("description", ""),
                "dir": skill_dir.name,
                "content": content,
                "body": strip_frontmatter(content),
            }
        )
    return skills


def discover_agents(source: Path) -> list[dict]:
    agents = []
    agents_dir = source / "agents"
    if not agents_dir.exists():
        return agents
    for agent_file in sorted(agents_dir.glob("*.md")):
        agents.append({"name": agent_file.stem})
    return agents


def generate_claude(skills: list[dict], path: Path, source_rel: str, source: Path):
    commands_dir = path / ".claude" / "commands"
    commands_dir.mkdir(parents=True, exist_ok=True)
    for skill in skills:
        cmd_file = commands_dir / f"{skill['dir']}.md"
        cmd_file.write_text(
            f"@{source_rel}/skills/{skill['dir']}/SKILL.md\n", encoding="utf-8"
        )
    print(f"  claude: {len(skills)} commands em {commands_dir}")

    agents = discover_agents(source)
    if agents:
        agents_dir = path / ".claude" / "agents"
        agents_dir.mkdir(parents=True, exist_ok=True)
        for agent in agents:
            agent_file = agents_dir / f"{agent['name']}.md"
            agent_file.write_text(
                f"@{source_rel}/agents/{agent['name']}.md\n", encoding="utf-8"
            )
        print(f"  claude: {len(agents)} agents em {agents_dir}")


def generate_opencode(skills: list[dict], path: Path, source_rel: str):
    commands_dir = path / ".opencode" / "commands"
    commands_dir.mkdir(parents=True, exist_ok=True)
    for skill in skills:
        cmd_file = commands_dir / f"{skill['dir']}.md"
        cmd_file.write_text(
            f"@{source_rel}/skills/{skill['dir']}/SKILL.md\n", encoding="utf-8"
        )
    print(f"  opencode: {len(skills)} commands em {commands_dir}")


def generate_cursor(skills: list[dict], path: Path):
    rules_dir = path / ".cursor" / "rules"
    rules_dir.mkdir(parents=True, exist_ok=True)
    for skill in skills:
        mdc_file = rules_dir / f"sspdd-{skill['dir']}.mdc"
        desc = skill["description"]
        body = skill["body"]
        content = f"---\ndescription: {desc}\n---\n{body}"
        mdc_file.write_text(content, encoding="utf-8")
    print(f"  cursor: {len(skills)} regras .mdc em {rules_dir}")


def generate_copilot(skills: list[dict], path: Path):
    gh_dir = path / ".github"
    gh_dir.mkdir(parents=True, exist_ok=True)
    parts = ["# SSPDD Copilot Instructions\n"]
    for skill in skills:
        parts.append(f"## /{skill['name']}\n\n{skill['body']}\n")
    out = gh_dir / "copilot-instructions.md"
    out.write_text("\n".join(parts), encoding="utf-8")
    print(f"  copilot: {out}")


def main():
    parser = argparse.ArgumentParser(
        description="Gera arquivos de plataforma para workspace SSPDD"
    )
    parser.add_argument(
        "--platform",
        required=True,
        choices=["claude", "cursor", "copilot", "opencode", "all"],
    )
    parser.add_argument("--path", required=True, help="Raiz do workspace destino")
    parser.add_argument(
        "--source", required=True, help="Caminho para .agents/ do framework"
    )
    args = parser.parse_args()

    source = Path(args.source).resolve()
    if not source.exists():
        print(f"ERRO: source '{source}' não encontrado.", file=sys.stderr)
        sys.exit(1)

    path = Path(args.path).resolve()
    path.mkdir(parents=True, exist_ok=True)

    # source_rel: caminho relativo de .agents/ a partir da raiz do workspace
    try:
        source_rel = source.relative_to(path).as_posix()
    except ValueError:
        source_rel = ".agents"

    skills = discover_skills(source)
    if not skills:
        print("[AVISO] Nenhum SKILL.md encontrado em source/skills/.")

    platforms = (
        ["claude", "cursor", "copilot", "opencode"]
        if args.platform == "all"
        else [args.platform]
    )

    for plat in platforms:
        if plat == "claude":
            generate_claude(skills, path, source_rel, source)
        elif plat == "opencode":
            generate_opencode(skills, path, source_rel)
        elif plat == "cursor":
            generate_cursor(skills, path)
        elif plat == "copilot":
            generate_copilot(skills, path)

    print(f"OK - Plataformas geradas: {', '.join(platforms)}")


if __name__ == "__main__":
    main()
