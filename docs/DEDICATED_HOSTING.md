# The Malefic Veil — long-running dedicated host (development gate)

**Status:** functional headless Java 17 prototype host with persistent *single-city arena* world overlays, low-frequency autosave, admin console, hashed LAN identities, PvP structure damage and offline raid grace. This is **not** a vetted internet service or a fully streamed continental MMORPG. The continent generation and population records are macro blueprints; characters and vehicles still play in the 132×132 city sector.

## Host and persistence

Compile from repository root (JDK 17+):

```sh
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/main/java/veil/*.java
java -Dveil.bind=127.0.0.1 -Dveil.offlineGraceMinutes=30 \
     -cp out veil.LanServer 7777 20261020 server-data
```

`veil.bind` defaults to `127.0.0.1` for the dedicated entrypoint. A bind of `0.0.0.0` opens the plaintext protocol to other machines; **do not put this prototype directly on the public internet.** First add TLS (or private-network authenticated tunneling), independent session authentication, anti-amplification, enforced admission/capacity tests, reconnect/backpressure and a realistic adversarial soak. Normal in-game `HOST LAN` remains a disposable nonpersistent match and is not governed by the persistent moderator.

`server-data/realm.dat` stores a versioned, seed-checked, CRC-verified snapshot of devices, built-owner identity hashes, placed car coordinates/fuel/durability, pickups, NPCs and offline-presence timestamps. The host saves every 60 seconds and on graceful close. Each save is staged then atomically renamed where the filesystem supports it, with `realm.bak` preserving a previously validated copy. A corrupt primary recovers from a valid backup (and restores that backup to the primary); an incompatible seed or corrupt primary **and** backup refuses to load instead of resetting the player world. Save contents are bound to the existing world generation model/collision, so changing the world model without migration can intentionally block loading. Character inventory and economic progression are still **session-only**, and are explicitly not yet recovered after restart.

`moderation.properties` persists allowlist and ban decisions separately; `audit.log` appends timestamped administrative actions and connection events. Backup `server-data/` outside the game process as well. The single rotating `.bak` is not a historical retention policy. For a 24/7 service use a filesystem with atomic rename, persistent storage, off-machine backups, dedicated unprivileged service identity, disk monitoring and crash-loop bounds.

## Console administration

| Command | Behavior |
|---|---|
| `status`, `players` | Print uptime, connected peers, entity counts and stable account IDs |
| `save` | Synchronously write/read-back verified world snapshot |
| `kick <peer-id>` | Disconnect an active peer |
| `ban <account-id>`, `unban <account-id>` | Persist ban; active matching account is kicked |
| `allow <account-id>`, `deny <account-id>` | Modify persistent allowlist membership |
| `allowlist on`, `allowlist off` | Control authenticated admission to dedicated mode |
| `shutdown` | Close listener and connections; save world before exit |

Commands are local standard-input console only, **not** network packets any client can fake. Dedicated mode requires `AUTH` before gameplay commands; the host rejects duplicate active account identities, unauthorized entries, oversized lines and over 120 commands per second per connection. Maximum concurrent sockets currently 32; game-scaled performance, NAT/TLS, DDoS controls, role-based remote admin, IP bans, moderation appeals, world rollback tooling and ban evasion defenses remain release gates.

The client creates a cryptographically random secret in `~/.malefic-veil/identities/` for each host:port, sends it to the LAN host, and uses the SHA-256-derived account identity for long-lived build ownership. Password-equivalent tokens must remain private; anyone capturing a raw AUTH token on the current plaintext transport could impersonate that account. Changing hostname spelling or port currently yields a different local identity; account migration is not implemented. Multiple clients running from the same local OS profile and targeting the same host:port share this token; in disposable LAN mode the duplicate becomes a guest, while the dedicated server rejects the second login.

## Offline base vulnerability

- Build ownership is bound to the hashed authenticated account ID, not the ephemeral socket player number.
- While an owner is online, a base can take PvP damage. On last logout, **the base remains vulnerable for 30 minutes by default**, preventing instant logoff-shield abuse.
- After the configurable `veil.offlineGraceMinutes` window, PvP damage to owner-authenticated devices is denied. The shooter spends the fired round and receives a `SHIELDED` combat receipt.
- The first owner rejoin immediately disables offline protection. UTC logout timestamps persist across clean restarts. A crash that persisted an online owner resets grace from startup, avoiding a sudden unannounced opening.
- Guest-built structures are not eligible for durable ownership and cannot be made protected by guessing a disconnected peer ID. Structure damage obeys the same server authority checks as the rest of combat.

**Open balance questions:** configurable scheduled raid windows, team/clan owner rules, explicit one-way siege declaration, long-abandoned territory reclamation, allowed structure repairs, controlled demolition and decay, PvE/environmental damage rules, anti-alt exploitation, rollback after exploit, and a player-visible shield countdown. The current rule protects the small prototype's device layer; it is not yet a generalized multistory building/territory claim system.

## Vehicles

The car vertical slice is playable via `Q` to enter/exit a nearby car; `W/S` throttle/reverse, `A/D` steer. Drivers cannot fire/build, spoof car coordinates or switch occupancy without the host. Cars use real source-licensed Kenney truck GLB geometry, fuel, capped acceleration/speed, collision damage, seat occupancy and emergency disconnect cleanup. Host snapshots replicate car pose; surviving vehicles and fuel persist. Car refueling, repairs, passengers, cargo, wreck recovery, traffic AI, terrain-dependent traction, traffic sound and intercity travel are subsequent gates.

## Example systemd unit (Linux, adapt paths and account)

```ini
[Unit]
Description=The Malefic Veil dedicated host
After=network-online.target
Wants=network-online.target
StartLimitIntervalSec=300
StartLimitBurst=3

[Service]
Type=simple
User=malefic
Group=malefic
WorkingDirectory=/srv/malefic-veil
ExecStart=/usr/bin/java -Dveil.bind=127.0.0.1 -Dveil.offlineGraceMinutes=30 -cp out veil.LanServer 7777 20261020 server-data
Restart=on-failure
RestartSec=10
TimeoutStopSec=40
NoNewPrivileges=yes
PrivateTmp=yes
ProtectSystem=strict
ReadWritePaths=/srv/malefic-veil/server-data
StandardInput=null

[Install]
WantedBy=multi-user.target
```

The service manager—not the JVM—keeps a process alive across an unexpected exit. Because standard input is disabled, interactive administration is not available via this example unit; use an attached console during development, or implement a separately authenticated local admin socket before production. SIGTERM invokes the shutdown hook; also schedule independent snapshots/retention and test restore on a staging machine. A detached server on someone's personal computer is not automatically online 24/7.

## Required release gates

1. **World streaming:** map parcel world coordinates into stable collision sectors, materialize continent terrain/coast/roads/plots and generated world buildings, stream only interest-radius chunks, maintain 3D protected masks and all city connections. The new `ContinentPopulation` gives deterministic city/wilderness player and NPC spawn candidates, not active actors in those sectors.
2. **Identity and network:** encrypted transport, authenticated reconnect/session tickets and custody for account secrets, bans/reputation not trivially evadable, rate/concurrency protections under hostile traffic, transport version migration and motion interpolation with vehicles at scale.
3. **Persistence:** per-account inventories/recovery, clans/team claims, device and construction mutations across chunks, transactional building undo/redo, backups retention and crash consistency tests including disk full, power loss, restart during save and world migration.
4. **QoL/administration:** claim/upkeep preview and raid timers visible in UI; dedicated console/API permission roles, player reports/appeals, kick reason, audit search, rollback, moderation tooling, server metrics/alarms and graceful maintenance windows.
5. **Real hosting acceptance:** minimum 4–16 clients, 2+ hour soak, loss/latency/load injection, full host restart and reconnect without duplication, no item/device corruption, real human desktop traversal/vehicle tests and explicit operator signoff.


## Local-first TLS implementation (2026-10-28)

See [current dedicated commands](network/DEDICATED_SERVER.md), [security gate](network/SECURITY_MODEL.md) and [fresh test receipts](network/TEST_RECEIPTS.md). TLS and signed installation identity are implemented in the existing host; local strict-TLS tests currently fail certificate pin verification. This does not supersede the existing persistence/backup limits or establish Internet readiness.
