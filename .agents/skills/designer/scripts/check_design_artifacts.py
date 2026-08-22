"""
Verifica que os artefatos obrigatorios de design (screen-map.md, design-tokens.json
e ao menos um prototipo .html) existem ao lado do Design Brief.
Uso: check_design_artifacts.py --artifact <design-brief.md>
Saida: erros em stderr, exit 1 se algum artefato obrigatorio estiver ausente.
"""

import sys
from pathlib import Path


def main():
    if len(sys.argv) < 3 or sys.argv[1] != "--artifact":
        print("Uso: check_design_artifacts.py --artifact <design-brief.md>", file=sys.stderr)
        sys.exit(2)

    brief_path = Path(sys.argv[2])
    feature_dir = brief_path.parent / brief_path.stem.replace("-design-brief", "")

    errors = []

    screen_map_path = feature_dir / "screen-map.md"
    if not screen_map_path.exists():
        errors.append(f"ERRO: screen-map.md ausente em '{screen_map_path}'.")

    tokens_path = feature_dir / "design-tokens.json"
    if not tokens_path.exists():
        errors.append(f"ERRO: design-tokens.json ausente em '{tokens_path}'.")

    prototypes_dir = feature_dir / "prototypes"
    if not prototypes_dir.exists() or not any(prototypes_dir.glob("*.html")):
        errors.append(f"ERRO: nenhum prototipo .html encontrado em '{prototypes_dir}'.")

    for msg in errors:
        print(msg, file=sys.stderr)

    sys.exit(0 if not errors else 1)


if __name__ == "__main__":
    main()
