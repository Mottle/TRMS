# Documentation Standards

Horizon treats public API and user-facing configuration as documented behavior.

## When Documentation Must Change

Update documentation in the same change when you:

- add or remove public Extension API types;
- add or remove Horizon Plugin API methods;
- add configuration keys;
- change default values that affect operators;
- add commands;
- change threading, compatibility, or fallback behavior;
- add user-facing scripting APIs;
- change extension descriptor fields or loading rules.

## Where To Document

| Change | Location |
| --- | --- |
| Extension API usage | `horizon-extension/docs/` |
| Extension descriptor/loading | `horizon-extension/docs/extension-descriptor.md` |
| Plugin-facing Bukkit/Paper API | `docs/horizon-plugin-api.md` |
| Server config and diagnostics | `docs/horizon-config.md` |
| Scripting | `docs/scripting.md` |
| NeoForge migration behavior | `docs/neoforge-migration.md` |
| Region file formats | `detail/` |

## Public API Rules

For public API types:

- add JavaDoc for the type and non-obvious methods;
- mention threading requirements when live world state is involved;
- mention fallback behavior when legacy Bukkit/Paper APIs cannot represent Horizon data;
- add the type to `api-reference.md` or mark it internal with an annotation and comment;
- include a minimal example when the API has ordering, lifecycle, or ownership rules.

## Configuration Rules

For configuration keys:

- generated `horizon.toml` comments should explain the key locally;
- `docs/horizon-config.md` should explain the operator-facing purpose and risk;
- byte-size values should document accepted suffixes;
- disabled-by-default performance features should state why they are not default;
- incompatible combinations should be explicit.

## Review Checklist

Before merging API or config changes, check:

- README links still point to valid docs.
- Markdown links resolve locally.
- `api-reference.md` covers the public extension API surface.
- examples compile conceptually against the documented artifact coordinates.
- docs do not promise behavior that tests do not cover.

Run the repository contract check before committing documentation or public API
changes:

```bash
./gradlew checkDocumentation
```

The check validates local Markdown links, exact public Extension API type coverage,
presence of type-level public API Javadocs, documented project versions, obsolete region-file
configuration, and Horizon scripting command references. It is also part of the root
`check` lifecycle.
