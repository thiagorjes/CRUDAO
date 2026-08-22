"""Testes de validate_skills.py — validação estrutural de SKILL.md e DRs."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from validate_skills import validate_skill, main  # noqa: E402

VALID_SKILL_MD = """---
name: exemplo
description: Skill de exemplo mínima e válida usada como referência para contributors.
canvas-dimensions: [R, E]
input-artifacts:
  - docs/prd/exemplo-prd.md
output-artifacts:
  - docs/techspec/exemplo-techspec.md
---

## Objetivo

Descrição do objetivo da skill.

## Pré-condições

- PRD deve existir.

## Workflow

Passos do workflow.

## Artefatos

Lista de artefatos de entrada e saída.

## Canvas

Dimensões atualizadas.

## Handoff

Próximos passos.
"""


def write_skill(tmp_path: Path, content: str, skill_name: str = "exemplo") -> Path:
    skill_dir = tmp_path / skill_name
    skill_dir.mkdir(parents=True, exist_ok=True)
    skill_md = skill_dir / "SKILL.md"
    skill_md.write_text(content, encoding="utf-8")
    return skill_md


def test_skill_md_valido_sem_erros(tmp_path):
    skill_md = write_skill(tmp_path, VALID_SKILL_MD)
    assert validate_skill(skill_md) == []


def test_canvas_dimensions_invalido(tmp_path):
    content = VALID_SKILL_MD.replace(
        "canvas-dimensions: [R, E]", "canvas-dimensions: [X, E]"
    )
    skill_md = write_skill(tmp_path, content)
    errors = validate_skill(skill_md)
    assert any("canvas-dimensions inválido" in e for e in errors)


def test_secao_obrigatoria_ausente(tmp_path):
    content = VALID_SKILL_MD.replace("## Handoff\n\nPróximos passos.\n", "")
    skill_md = write_skill(tmp_path, content)
    errors = validate_skill(skill_md)
    assert any("Handoff" in e for e in errors)


def test_frontmatter_ausente(tmp_path):
    content = "## Objetivo\n\nSem frontmatter.\n"
    skill_md = write_skill(tmp_path, content)
    errors = validate_skill(skill_md)
    assert any("Frontmatter YAML ausente" in e for e in errors)


def test_campo_frontmatter_faltando(tmp_path):
    content = VALID_SKILL_MD.replace(
        "output-artifacts:\n  - docs/techspec/exemplo-techspec.md\n", ""
    )
    skill_md = write_skill(tmp_path, content)
    errors = validate_skill(skill_md)
    assert any("output-artifacts" in e for e in errors)


def test_main_exit_0_skills_validas(tmp_path, capsys):
    write_skill(tmp_path, VALID_SKILL_MD, "exemplo1")
    write_skill(tmp_path, VALID_SKILL_MD, "exemplo2")
    old_argv = sys.argv
    sys.argv = ["validate_skills.py", str(tmp_path)]
    try:
        try:
            main()
        except SystemExit as e:
            assert e.code == 0
    finally:
        sys.argv = old_argv


def test_main_exit_1_skill_invalida(tmp_path, capsys):
    content = VALID_SKILL_MD.replace(
        "canvas-dimensions: [R, E]", "canvas-dimensions: [X]"
    )
    write_skill(tmp_path, content)
    old_argv = sys.argv
    sys.argv = ["validate_skills.py", str(tmp_path)]
    try:
        try:
            main()
        except SystemExit as e:
            assert e.code == 1
    finally:
        sys.argv = old_argv
    out = capsys.readouterr()
    assert "canvas-dimensions inválido" in out.err
