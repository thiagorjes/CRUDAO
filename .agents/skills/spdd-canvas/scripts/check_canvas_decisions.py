"""
Verifica que cada dimensão do REASONS Canvas tem linha '> Decisões:'.
Linha pode ser '> Decisões: —' (explicitamente vazia) — isso é válido.
Saída: erros em stderr, exit 1 se inválido.
"""

import re
import sys
from pathlib import Path

DECISIONS_RE = re.compile(r'^>\s+Decis[õo]es:')
DIM_HEADING_RE = re.compile(r'^##\s+[REASONS]\s+—')


def main():
    if len(sys.argv) < 3 or sys.argv[1] != "--canvas":
        print("Uso: check_canvas_decisions.py --canvas <arquivo>", file=sys.stderr)
        sys.exit(2)

    canvas_path = Path(sys.argv[2])
    if not canvas_path.exists():
        print(f"ERRO: Canvas '{canvas_path}' não encontrado.", file=sys.stderr)
        sys.exit(2)

    content = canvas_path.read_text(encoding="utf-8")
    lines = content.splitlines()

    errors = []
    current_dim = None
    dim_has_decisions = False

    for line in lines:
        if DIM_HEADING_RE.match(line):
            if current_dim and not dim_has_decisions:
                errors.append(f"ERRO: Dimensão '{current_dim}' sem linha '> Decisões:'")
            current_dim = line.strip()
            dim_has_decisions = False
        elif current_dim and DECISIONS_RE.match(line):
            dim_has_decisions = True

    if current_dim and not dim_has_decisions:
        errors.append(f"ERRO: Dimensão '{current_dim}' sem linha '> Decisões:'")

    for msg in errors:
        print(msg, file=sys.stderr)

    sys.exit(0 if not errors else 1)


if __name__ == "__main__":
    main()
