# Frontend Testing Notes

## Commands

Run tests one-shot:

```bash
pnpm test --watch=false
```

Run build:

```bash
pnpm build
```

## Notes

- `pnpm test -- --run` puede fallar por flags no soportados en esta config.
- Usar `--watch=false` para CI/local no interactivo.
