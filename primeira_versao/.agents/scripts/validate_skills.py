"""
Valida estrutura de todos os SKILL.md em .agents/skills/.
Verifica: frontmatter obrigatório, canvas-dimensions válidas, 6 seções obrigatórias.

Uso: python validate_skills.py <skills_dir>
Exit 0 = todos válidos | Exit 1 = erros encontrados
"""

import re
import sys
from pathlib import Path

VALID_CANVAS_DIMS = {"R", "E", "A", "S", "O", "N"}

VALID_DR_STATUS = {"accepted", "superseded", "deprecated"}
REQUIRED_DR_FRONTMATTER = ["id", "type", "status", "date"]
REQUIRED_DR_SECTIONS = [
    "## Decisão",
    "## Motivação",
    "## Consequências",
    "## Alternativas Consideradas",
]

REQUIRED_FRONTMATTER = [
    "name",
    "description",
    "canvas-dimensions",
    "input-artifacts",
    "output-artifacts",
]

REQUIRED_SECTIONS = [
    "## Objetivo",
    "## Pré-condições",
    "## Workflow",
    "## Artefatos",
    "## Canvas",
    "## Handoff",
]

FRONTMATTER_RE = re.compile(r"^---\n(.*?)\n---", re.DOTALL)
CANVAS_DIMS_RE = re.compile(r"canvas-dimensions:\s*\[([^\]]*)\]")
HEADER_RE = re.compile(r"^#{1,6}\s+(.+)$", re.MULTILINE)


def parse_frontmatter(content: str) -> dict:
    m = FRONTMATTER_RE.match(content)
    if not m:
        return {}
    fm: dict = {}
    for line in m.group(1).splitlines():
        if ":" in line:
            key, _, val = line.partition(":")
            fm[key.strip()] = val.strip()
    return fm


def validate_skill(skill_md: Path) -> list[str]:
    skill_name = skill_md.parent.name
    errors = []

    try:
        content = skill_md.read_text(encoding="utf-8")
    except Exception as e:
        return [f"ERRO: [{skill_name}] Não foi possível ler SKILL.md: {e}"]

    # Verificar frontmatter presente
    if not FRONTMATTER_RE.match(content):
        errors.append(f"ERRO: [{skill_name}] Frontmatter YAML ausente ou malformado")
        return errors

    fm = parse_frontmatter(content)

    # Verificar campos obrigatórios do frontmatter
    for field in REQUIRED_FRONTMATTER:
        if field not in fm:
            errors.append(f"ERRO: [{skill_name}] Frontmatter faltando campo '{field}'")

    # Verificar canvas-dimensions
    m = CANVAS_DIMS_RE.search(content)
    if m:
        raw_dims = m.group(1)
        dims = [d.strip() for d in raw_dims.split(",") if d.strip()]
        for d in dims:
            if d not in VALID_CANVAS_DIMS:
                errors.append(f"ERRO: [{skill_name}] canvas-dimensions inválido: {d}")
    elif "canvas-dimensions" in fm:
        # Empty list [] is valid
        pass

    # Verificar 6 seções obrigatórias
    headings = set(HEADER_RE.findall(content))
    for section in REQUIRED_SECTIONS:
        bare = re.sub(r"^#+\s*", "", section).strip()
        if not any(bare == h.strip() for h in headings):
            errors.append(
                f'ERRO: [{skill_name}] Seção obrigatória ausente: "{section}"'
            )

    return errors


def validate_dr(dr_md: Path) -> list[str]:
    """Valida um Decision Record: não é SKILL.md, mas tem schema próprio (frontmatter + 4 seções)."""
    dr_name = dr_md.name
    errors = []

    try:
        content = dr_md.read_text(encoding="utf-8")
    except Exception as e:
        return [f"ERRO: [{dr_name}] Não foi possível ler: {e}"]

    if not FRONTMATTER_RE.match(content):
        errors.append(f"ERRO: [{dr_name}] Frontmatter YAML ausente ou malformado")
        return errors

    fm = parse_frontmatter(content)

    for field in REQUIRED_DR_FRONTMATTER:
        if field not in fm:
            errors.append(f"ERRO: [{dr_name}] Frontmatter faltando campo '{field}'")

    status = fm.get("status")
    if status is not None and status not in VALID_DR_STATUS:
        errors.append(f"ERRO: [{dr_name}] status inválido: {status}")

    headings = set(HEADER_RE.findall(content))
    for section in REQUIRED_DR_SECTIONS:
        bare = re.sub(r"^#+\s*", "", section).strip()
        if not any(bare == h.strip() for h in headings):
            errors.append(f'ERRO: [{dr_name}] Seção obrigatória ausente: "{section}"')

    return errors


def validate_dr_index(constitution_path: Path, decisions_dir: Path) -> list[str]:
    """Verifica que DRs referenciados no índice existem em docs/decisions/."""
    warnings = []
    if not constitution_path.exists() or not decisions_dir.exists():
        return warnings

    content = constitution_path.read_text(encoding="utf-8")
    # Find markdown links: [ADR-001](../docs/decisions/ADR-001-...)
    link_re = re.compile(r"\[([A-Z]{2,3}-\d+)\]\(([^)]+)\)")
    for m in link_re.finditer(content):
        dr_id, path_str = m.group(1), m.group(2)
        # Resolve relative to constitution.md parent
        ref = (constitution_path.parent / path_str).resolve()
        if not ref.exists():
            warnings.append(
                f"AVISO: [{dr_id}] referenciado no índice mas arquivo não encontrado: {path_str}"
            )

    return warnings


def main():
    if len(sys.argv) < 2:
        print("Uso: validate_skills.py <skills_dir>", file=sys.stderr)
        sys.exit(2)

    skills_dir = Path(sys.argv[1])
    if not skills_dir.exists():
        print(f"ERRO: Diretório '{skills_dir}' não encontrado.", file=sys.stderr)
        sys.exit(2)

    all_errors: list[str] = []

    skill_mds = sorted(skills_dir.rglob("SKILL.md"))
    if not skill_mds:
        print(f"AVISO: Nenhum SKILL.md encontrado em '{skills_dir}'.", file=sys.stderr)
        sys.exit(0)

    for skill_md in skill_mds:
        errors = validate_skill(skill_md)
        all_errors.extend(errors)

    # Validar Decision Records (tipo especial: não é SKILL.md, mas tem schema próprio)
    constitution = skills_dir.parent.parent / "memory" / "constitution.md"
    decisions = skills_dir.parent.parent / "docs" / "decisions"
    if decisions.exists():
        for dr_md in sorted(decisions.glob("*.md")):
            all_errors.extend(validate_dr(dr_md))

    # Opcional: verificar integridade do índice de DRs
    all_errors.extend(validate_dr_index(constitution, decisions))

    for msg in all_errors:
        print(msg, file=sys.stderr)

    if all_errors:
        error_count = sum(1 for e in all_errors if e.startswith("ERRO:"))
        warn_count = sum(1 for e in all_errors if e.startswith("AVISO:"))
        print(
            f"\n{len(skill_mds)} skills verificadas — {error_count} erro(s), {warn_count} aviso(s).",
            file=sys.stderr,
        )
        if error_count > 0:
            sys.exit(1)
    else:
        print(f"{len(skill_mds)} skills verificadas — todas válidas.")

    sys.exit(0)


if __name__ == "__main__":
    main()
