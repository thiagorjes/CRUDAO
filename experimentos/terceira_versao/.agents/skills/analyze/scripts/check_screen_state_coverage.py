"""
Verifica cobertura entre docs/design/[feature]/screen-map.md e PRD/TechSpec (Fase 1.5 de /analyze).

Checa:
1. Linhas da tabela "## Cobertura de RFs" com Status != "coberto" (RF sem tela mapeada)
2. Itens da seção "## Gaps Identificados" (gaps já registrados pelo agente prototipador)
3. RFs cobertos no screen-map que não aparecem em lugar nenhum na TechSpec (RF sem decisão técnica)

Não substitui a leitura manual da Fase 1.5 (granularidade de estado por tela exige
julgamento) — é um piso automatizado que pega os casos óbvios.

Uso: python check_screen_state_coverage.py --screen-map <screen-map.md> [--techspec <techspec.md>]
Exit 0 = nenhum gap óbvio | Exit 1 = gaps encontrados (impresso em stderr)
Se --screen-map não existir: exit 0 silencioso (feature sem /designer, gate não se aplica)
"""

import re
import sys
from pathlib import Path

RF_RE = re.compile(r"\bRF-\d{3}\b")
COVERAGE_ROW_RE = re.compile(
    r"^\|\s*(RF-\d{3})\s*\|(.*)\|\s*([^|]*)\s*\|\s*([^|]*)\s*\|\s*$", re.MULTILINE
)


def parse_args(args: list[str]) -> dict:
    out = {"screen-map": None, "techspec": None}
    i = 0
    while i < len(args):
        if args[i] == "--screen-map" and i + 1 < len(args):
            out["screen-map"] = Path(args[i + 1])
            i += 2
        elif args[i] == "--techspec" and i + 1 < len(args):
            out["techspec"] = Path(args[i + 1])
            i += 2
        else:
            i += 1
    return out


def extract_section(text: str, heading: str) -> str:
    pattern = rf"^##\s*{re.escape(heading)}\s*$(.*?)(?=^##\s|\Z)"
    m = re.search(pattern, text, re.MULTILINE | re.DOTALL)
    return m.group(1) if m else ""


def main():
    opts = parse_args(sys.argv[1:])
    screen_map_path = opts["screen-map"]

    if not screen_map_path:
        print("ERRO: --screen-map é obrigatório", file=sys.stderr)
        sys.exit(2)

    if not screen_map_path.exists():
        # Feature sem /designer — gate de protótipo não se aplica.
        sys.exit(0)

    text = screen_map_path.read_text(encoding="utf-8")
    problems: list[str] = []

    # 1. Tabela "Cobertura de RFs": qualquer status diferente de "coberto"
    coverage_section = extract_section(text, "Cobertura de RFs")
    rfs_cobertos: set[str] = set()
    for match in COVERAGE_ROW_RE.finditer(coverage_section):
        rf, _desc, _telas, status = match.groups()
        status_norm = status.strip().lower()
        if status_norm == "coberto":
            rfs_cobertos.add(rf)
        else:
            problems.append(
                f"screen-map: {rf} com status '{status.strip()}' (sem tela mapeada ou incompleto)"
            )

    # 2. Seção "Gaps Identificados": qualquer bullet não vazio
    gaps_section = extract_section(text, "Gaps Identificados")
    for line in gaps_section.splitlines():
        line = line.strip()
        if line.startswith("-") and line.lstrip("- ").strip():
            problems.append(f"screen-map (Gaps Identificados): {line.lstrip('- ').strip()}")

    # 3. RFs cobertos no screen-map sem menção na TechSpec
    techspec_path = opts["techspec"]
    if techspec_path and techspec_path.exists() and rfs_cobertos:
        techspec_rfs = set(RF_RE.findall(techspec_path.read_text(encoding="utf-8")))
        missing = rfs_cobertos - techspec_rfs
        for rf in sorted(missing):
            problems.append(
                f"{rf} tem tela mapeada no screen-map mas não é referenciado na TechSpec "
                "(possível gap de decisão técnica)"
            )

    if problems:
        for p in sorted(set(problems)):
            print(f"AVISO: {p}", file=sys.stderr)
        sys.exit(1)

    sys.exit(0)


if __name__ == "__main__":
    main()
