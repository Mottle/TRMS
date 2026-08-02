# TRMS design

This directory records decisions shared by the `extension` and `mod` runtime
artifacts in the single TRMS Gradle build. Both depend on the pure Java
`common` module, but neither endpoint imports the other's implementation; the
documents here are their shared behavioral contract.

## Accepted decisions

- [ADR-0001: ceramic single-layer mold carving](adr/0001-ceramic-single-layer-mold-carving.md)
- [ADR-0002: visual molten mold fills](adr/0002-visual-molten-mold-fills.md)
- [ADR-0003: mold cooling and weapon parts](adr/0003-mold-cooling-and-weapon-parts.md)
- [Mold pattern v1 test contract](contracts/mold-pattern-v1.properties)

## Shared vocabulary

- [Glossary](glossary.md)
