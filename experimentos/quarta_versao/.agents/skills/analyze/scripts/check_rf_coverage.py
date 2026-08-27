"""
Verifica que todos os RFs do PRD aparecem no documento de Tasks (RF->Task coverage).
Uso: python check_rf_coverage.py --prd <prd.md> [--tasks <tasks.md>]
Exit 0 = cobertura completa | Exit 1 = gaps encontrados
"""

import re
import sys
from pathlib import Path

RF_RE = re.compile(r'\bRF-\d{3}\b')


def extract_rfs(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return set(RF_RE.findall(path.read_text(encoding="utf-8")))


def main():
    args = sys.argv[1:]
    prd_path = None
    tasks_path = None

    i = 0
    while i < len(args):
        if args[i] == "--prd" and i + 1 < len(args):
            prd_path = Path(args[i + 1])
            i += 2
        elif args[i] == "--tasks" and i + 1 < len(args):
            tasks_path = Path(args[i + 1])
            i += 2
        else:
            i += 1

    if not prd_path:
        print("ERRO: --prd é obrigatório", file=sys.stderr)
        sys.exit(2)

    prd_rfs = extract_rfs(prd_path)
    if not prd_rfs:
        sys.exit(0)

    if tasks_path is None:
        # Inferir tasks do mesmo diretório nível acima
        # ex: docs/prd/feat-prd.md → docs/tasks/feat-tasks.md
        stem = prd_path.stem.replace("-prd", "")
        tasks_path = prd_path.parent.parent / "tasks" / f"{stem}-tasks.md"

    tasks_rfs = extract_rfs(tasks_path)

    gaps = prd_rfs - tasks_rfs
    if gaps:
        for rf in sorted(gaps):
            print(f"AVISO: {rf} presente no PRD mas sem task correspondente", file=sys.stderr)
        sys.exit(1)

    sys.exit(0)


if __name__ == "__main__":
    main()
