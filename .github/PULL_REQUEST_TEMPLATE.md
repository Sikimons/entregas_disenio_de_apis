## Resumen

<!-- Que cambia y por que. Si corrige un bug, cual era el sintoma real. -->

## Tipo de cambio

<!-- Debe coincidir con el prefijo principal de los commits de este PR -- ver CONTRIBUTING.md -->

- [ ] `fix` (patch)
- [ ] `feat` (minor)
- [ ] `feat!` / `BREAKING CHANGE` (major)
- [ ] `docs` / `refactor` / `test` / `chore` / `ci` / `perf` (no dispara release)

## Test plan

<!-- Como se verifico. "Deberia funcionar" no cuenta -- que se ejecuto realmente. -->

- [ ] `mvn test` (backend)
- [ ] `pnpm test` / `pnpm build` (frontend)
- [ ] Verificacion manual contra el stack real (`docker compose up`), si aplica
- [ ] Otro: <!-- describir -->

## Checklist

- [ ] La rama nace de `develop` (o de `main` solo si es un hotfix real)
- [ ] Los commits siguen Conventional Commits (ver CONTRIBUTING.md)
- [ ] Si toca `cd.yml`/`ci.yml`, se probo que el YAML es valido y el flujo de jobs sigue teniendo sentido
