# Mixed Authentication Operations

Horizon mixed authentication allows a server using the normal online/proxy login chain
to admit a failed online session as a separate, locked Horizon offline account. It is
disabled by default.

This is an account-recovery and mixed-access feature, not a replacement for a correctly
configured proxy or Mojang authentication. Enabling it changes the server's trust model:
any client that cannot complete the normal session check may reach the locked login
state and attempt to authenticate as a Horizon offline account.

## Login Model

With `[auth].mixed-mode-enabled = true`:

1. A profile accepted by the normal server or proxy chain remains a server profile and
   bypasses Horizon's password lock.
2. If the online session check fails, Horizon resolves the requested name as a separate
   offline identity.
3. The real in-game profile name receives `offline-profile-prefix`; with the default,
   requested name `liar` becomes `ofl_liar`.
4. The player enters locked state and must use `/login`, or `/register` when self
   registration is enabled.
5. Successful Horizon authentication unlocks that session. It does not turn the account
   into a Mojang-authenticated profile.

Plugin and Extension data that must distinguish these identities should use Horizon's
typed account ID. Server profiles use `profile:<uuid>` and Horizon offline accounts use
`horizon_offline:<normalized-name>`. See [Horizon Plugin API](horizon-plugin-api.md) for
the Bukkit-facing API.

`SERVER_PROFILE` means the configured server/proxy chain accepted the profile. On a
server running in vanilla offline mode or with incorrect proxy forwarding, that does
not prove Mojang authentication. Configure and secure the proxy path before relying on
mixed mode.

## Configuration

Section: `[auth]` in `config/horizon.toml`.

| Key | Default | Purpose and risk |
| --- | --- | --- |
| `mixed-mode-enabled` | `false` | Enables the online-failure to locked-offline fallback. Existing server/proxy profiles remain unchanged. |
| `offline-profile-prefix` | `ofl_` | Prefix added to the real GameProfile name of fallback accounts. Prefix plus requested name must fit Minecraft's 16-character limit. |
| `reserve-offline-profile-prefix` | `true` | Rejects raw login names already beginning with the configured prefix, reserving that namespace for generated offline profiles. |
| `allow-self-registration` | `false` | Allows an unregistered locked player to create a password with `/register`. Enabling it permits first-claim account registration. |
| `offline-allowlist-enabled` | `false` | Requires the typed offline account to be in Horizon's independent allowlist before it may join. |
| `offline-auth-timeout-seconds` | `45` | Kicks a still-locked player after this time. `0` disables timeout kicks. |
| `offline-auth-max-password-attempts` | `3` | Kicks after this many invalid `/login` attempts. `0` disables attempt-count kicks. |
| `offline-auth-min-password-length` | `1` | Minimum for new/reset passwords. Increase this for production; the default preserves compatibility rather than providing strong password requirements. |
| `offline-skin-lookup-enabled` | `false` | Looks up signed textures for fallback names using the configured Yggdrasil-compatible provider. |
| `offline-skin-yggdrasil-root` | empty | Provider API root. Empty disables lookup even when the boolean is enabled. |
| `offline-skin-lookup-timeout-ms` | `3000` | Total lookup timeout, clamped to `250..10000` milliseconds. |

Changing the offline identity or allowlist rules while sessions are active can require
affected fallback players to reconnect. Do not change the prefix after accounts have
been used unless all dependent data stores and operational procedures have been
migrated.

## Locked State

A locked player is present on the server but is not allowed to play normally. Horizon
sets movement abilities to zero and filters inbound game packets. Authentication
commands, their suggestions, keepalive/connection progress, teleport acknowledgement,
limited movement/use packets needed to keep client state coherent, respawn, and a known
voice-chat secret request remain processable. Other commands and gameplay packets do
not become an authentication bypass.

Only these player commands authenticate a locked session:

```text
/login <password>
/register <password> <confirmPassword>
```

Passwords in sensitive auth commands are redacted from Horizon command diagnostics.
Administrators should still treat console, proxy, plugin, and external audit logs as
sensitive and verify that those layers do not record raw command arguments.

## Administrator Commands

`/offlineauth` requires administrator-level command permission.

| Command | Purpose |
| --- | --- |
| `/offlineauth register <name> <password>` | Creates a missing offline account. Fails if it already exists. |
| `/offlineauth setpassword <name> <password>` | Creates or resets the account password. |
| `/offlineauth import <file> <defaultPassword>` | Imports requested names from a UTF-8 text file, registers missing accounts with one password, and adds them to the offline allowlist. |
| `/offlineauth allowlist on` | Enables Horizon's offline allowlist and persists the setting to `config/horizon.toml`. |
| `/offlineauth allowlist off` | Disables that allowlist and persists the setting. |
| `/offlineauth allowlist status` | Reports mode and entry count. |
| `/offlineauth allowlist add <name>` | Adds the normalized typed offline account. |
| `/offlineauth allowlist remove <name>` | Removes it. |
| `/offlineauth allowlist list` | Lists current typed account IDs. |

Import files are resolved under the server working directory, must be regular files,
and cannot escape that root. Files are limited to 10,000 lines. Use one requested name
per line; empty lines and lines starting with `#` are ignored. Existing passwords are
not changed. The operation can partially register accounts if the later allowlist write
fails, and reports that case explicitly so the command can be repaired and rerun.

The Horizon offline allowlist is independent of `whitelist.json`. Vanilla whitelist
rules still apply to the server profile presented to the normal server login path;
the Horizon list controls only fallback-account admission.

## Account Storage and Backups

Horizon stores account records in:

```text
config/horizon/offline-auth.properties
config/horizon/offline-allowlist.properties
```

Passwords are not stored as plaintext. Each account uses PBKDF2-HMAC-SHA256 with a
random 16-byte salt, 210,000 iterations, and a 256-bit result. The files are still
security-sensitive: possession enables offline password guessing and modification can
replace or remove account credentials.

- Restrict filesystem access to the server account.
- Back up both files atomically with `config/horizon.toml`.
- Do not hand-edit them while the server is running.
- Test restore procedures, including the configured prefix and allowlist mode.
- Reset shared/default import passwords after onboarding users.

The account UUID is deterministically derived from the typed account ID, so restoring
the account and configuration files preserves identity. Renaming the requested account
or changing normalization/prefix rules is not an account migration mechanism.

## Offline Skin Lookup

When enabled, Horizon sends the requested fallback username to the configured
Yggdrasil-compatible service and requests a signed profile texture property. This has
two consequences:

- the provider learns fallback usernames and server request timing;
- clients must trust the provider's texture signatures and domains, commonly through a
  matching authlib-injector setup.

Vanilla clients do not normally accept non-Mojang texture signatures or arbitrary
texture domains. Keep lookup disabled unless the complete client/server trust chain is
configured and the privacy tradeoff is acceptable.

## Recommended Deployment

1. Fix and verify online-mode/proxy forwarding first.
2. Back up the server and authentication configuration.
3. Choose a stable prefix and keep prefix reservation enabled.
4. Keep self-registration disabled; preregister or import intended accounts.
5. Enable the independent offline allowlist before exposing the fallback publicly.
6. Raise the minimum password length and retain timeout/attempt limits.
7. Test online, fallback, wrong-password, timeout, reconnect, proxy, and restore paths
   with non-production accounts.
