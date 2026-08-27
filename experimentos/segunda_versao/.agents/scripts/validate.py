"""
SSPDD artifact validation engine.
Usage: python validate.py --mode input|output --rules RULES_JSON --artifact ARTIFACT_MD [--system SYSTEM]

Exit 0 = valid
Exit 1 = invalid (errors printed to stderr)
Exit 2 = configuration error (missing rules/artifact)
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

REGISTRY_ROW = re.compile(r"^\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|")
PLACEHOLDER_RE = re.compile(r"\{\{[A-Z_]+\}\}")
HEADER_RE = re.compile(r"^#{1,6}\s+(.+)$", re.MULTILINE)


def load_rules(rules_path: Path, system: str, feature: str, artifact: str) -> dict:
    if not rules_path.exists():
        print(f"ERRO: rules '{rules_path}' não encontrado.", file=sys.stderr)
        sys.exit(2)
    raw = rules_path.read_text(encoding="utf-8")
    # {{INPUT_ARTIFACT}} não é substituído aqui: o valor pode conter caracteres
    # especiais de JSON (ex.: barras invertidas em paths Windows) e quebraria o
    # parse. A substituição correta ocorre em run_custom_steps, já com o JSON
    # parseado, apenas dentro de listas de argumentos.
    raw = raw.replace("{{SYSTEM}}", system).replace("{{FEATURE}}", feature)
    try:
        return json.loads(raw)
    except json.JSONDecodeError as e:
        print(f"ERRO: validate-rules.json inválido: {e}", file=sys.stderr)
        sys.exit(2)


def parse_registry(state_path: Path) -> dict[str, str]:
    """Returns {artifact_path: status}."""
    registry: dict[str, str] = {}
    if not state_path.exists():
        return registry
    content = state_path.read_text(encoding="utf-8")
    in_registry = False
    for line in content.splitlines():
        if line.strip() == "## Artifact Registry":
            in_registry = True
            continue
        if in_registry and line.startswith("## "):
            break
        if in_registry:
            m = REGISTRY_ROW.match(line)
            if m:
                artifact = m.group(1).strip()
                status = m.group(3).strip()
                if artifact and artifact != "Artefato" and artifact != "---":
                    registry[artifact] = status
    return registry


def find_state_md(artifact_path: Path) -> Path:
    # Walk up from artifact looking for memory/state.md
    current = artifact_path.parent
    for _ in range(10):
        candidate = current / "memory" / "state.md"
        if candidate.exists():
            return candidate
        parent = current.parent
        if parent == current:
            break
        current = parent
    # Fallback: try cwd
    return Path("memory/state.md")


def check_required_sections(content: str, required: list[str]) -> list[str]:
    errors = []
    headings = set(HEADER_RE.findall(content))
    for section in required:
        # Strip leading '#' and spaces for comparison
        bare = re.sub(r"^#+\s*", "", section).strip()
        if not any(bare == h.strip() for h in headings):
            errors.append(f'ERRO: Seção obrigatória ausente: "{section}"')
    return errors


def check_placeholders(content: str) -> list[str]:
    found = PLACEHOLDER_RE.findall(content)
    if found:
        unique = list(dict.fromkeys(found))
        return [f"ERRO: Placeholder não substituído: {p}" for p in unique]
    return []


def check_id_patterns(content: str, patterns: dict[str, str]) -> list[str]:
    errors = []
    for id_type, pattern in patterns.items():
        found = re.findall(rf"\b{id_type}-[\d.]+\b", content)
        compiled = re.compile(f"^{pattern}$")
        for fid in found:
            if not compiled.match(fid):
                errors.append(f"ERRO: ID '{fid}' não corresponde ao padrão '{pattern}'")
    return errors


def check_gherkin(
    content: str, id_types: list[str], patterns: dict[str, str]
) -> list[str]:
    errors = []
    for id_type in id_types:
        pat = patterns.get(id_type, rf"{id_type}-\d{{3}}")
        ids_found = re.findall(rf"\b{pat}\b", content)
        lines = content.splitlines()
        for rf_id in ids_found:
            # Find line index of this ID
            rf_line_idx = next(
                (i for i, line in enumerate(lines) if rf_id in line), None
            )
            if rf_line_idx is None:
                continue
            # Search within 20 lines after for Gherkin keywords (pt_BR or en_US)
            window = "\n".join(lines[rf_line_idx : rf_line_idx + 20])
            has_gherkin = bool(
                re.search(
                    r"(\*\*Dado que\*\*|Given\b|\*\*When\*\*|\*\*Quando\*\*)", window
                )
            )
            if not has_gherkin:
                errors.append(f"ERRO: {rf_id} não possui critério de aceite Gherkin")
    return errors


def run_custom_steps(steps: list[dict], artifact: str, cwd: Path) -> list[str]:
    messages = []
    for step in steps:
        script = step.get("script", "")
        args = [a.replace("{{INPUT_ARTIFACT}}", artifact) for a in step.get("args", [])]
        on_failure = step.get("on_failure", "error")
        result = subprocess.run(
            [sys.executable, script, *args],
            capture_output=True,
            text=True,
            cwd=str(cwd),
        )
        if result.returncode != 0:
            prefix = "ERRO" if on_failure == "error" else "AVISO"
            for line in result.stderr.strip().splitlines():
                messages.append(f"{prefix}: [{step.get('name', script)}] {line}")
        elif result.stderr.strip():
            for line in result.stderr.strip().splitlines():
                messages.append(f"AVISO: [{step.get('name', script)}] {line}")
    return messages


def mode_input(rules: dict, artifact_path: Path) -> list[str]:
    errors = []
    modes = rules.get("modes", {})
    input_cfg = modes.get("input", {})

    state_md = find_state_md(artifact_path)
    registry = (
        parse_registry(state_md) if input_cfg.get("check_registry", False) else {}
    )

    required = input_cfg.get("required_artifacts", [])
    for req in required:
        req_path = Path(req)
        if not req_path.exists():
            # Try relative to artifact parent
            alt = artifact_path.parent / req
            if not alt.exists():
                errors.append(f"ERRO: Artefato obrigatório ausente: '{req}'")
                continue
        status = registry.get(req, None)
        if status is not None and status.startswith("stale:"):
            errors.append(f"ERRO: Artefato stale: {req} ({status})")

    steps = input_cfg.get("custom_steps", [])
    errors.extend(run_custom_steps(steps, str(artifact_path), Path.cwd()))

    return errors


def mode_output(rules: dict, artifact_path: Path) -> list[str]:
    if not artifact_path.exists():
        print(f"ERRO: artifact '{artifact_path}' não encontrado.", file=sys.stderr)
        sys.exit(2)

    content = artifact_path.read_text(encoding="utf-8")
    errors = []
    modes = rules.get("modes", {})
    output_cfg = modes.get("output", {})

    errors.extend(
        check_required_sections(content, output_cfg.get("required_sections", []))
    )

    if output_cfg.get("no_empty_placeholders", False):
        errors.extend(check_placeholders(content))

    id_patterns = output_cfg.get("id_patterns", {})
    if id_patterns:
        errors.extend(check_id_patterns(content, id_patterns))

    gherkin_ids = output_cfg.get("gherkin_required_for_ids", [])
    if gherkin_ids:
        errors.extend(check_gherkin(content, gherkin_ids, id_patterns))

    steps = output_cfg.get("custom_steps", [])
    errors.extend(run_custom_steps(steps, str(artifact_path), Path.cwd()))

    return errors


def main():
    parser = argparse.ArgumentParser(description="SSPDD artifact validator")
    parser.add_argument("--mode", required=True, choices=["input", "output"])
    parser.add_argument(
        "--rules", required=True, help="Caminho para validate-rules.json"
    )
    parser.add_argument(
        "--artifact", required=True, help="Caminho para o artefato Markdown"
    )
    parser.add_argument(
        "--system", default="", help="Nome do sistema (substitui {{SYSTEM}})"
    )
    args = parser.parse_args()

    artifact_path = Path(args.artifact)
    feature = (
        artifact_path.stem.replace("-prd", "")
        .replace("-techspec", "")
        .replace("-tasks", "")
    )

    rules = load_rules(Path(args.rules), args.system, feature, args.artifact)

    if args.mode == "input":
        errors = mode_input(rules, artifact_path)
    else:
        errors = mode_output(rules, artifact_path)

    for msg in errors:
        print(msg, file=sys.stderr)

    sys.exit(0 if not any(e.startswith("ERRO:") for e in errors) else 1)


if __name__ == "__main__":
    main()
