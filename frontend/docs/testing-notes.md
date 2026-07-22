# Notas de testing Frontend

## Comandos

Ejecutar tests one-shot:

```bash
pnpm test --watch=false
```

Ejecutar build:

```bash
pnpm build
```

## Notas

- `pnpm test -- --run` puede fallar por flags no soportados en esta config.
- Usar `--watch=false` para CI/local no interactivo.
