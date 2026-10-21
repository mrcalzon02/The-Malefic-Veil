# The Malefic Veil — separated-zone server, storage and character architecture
Status: implementation contract, not a declaration of production readiness. Scope excludes weapons and player construction, developed in parallel.

## Authority and topology

A realm comprises **one world coordinator**, **N zone simulators**, an **identity/profile service**, and **transactional persistence**. Start as distinct services within a single dedicated JVM with explicit interfaces and independent lifecycle; permit remote zone workers only when handoff and fencing pass the acceptance gates. A static zone grid partitions the 224×224 continental macro layout into stable contiguous zones of 4×4 existing 8×8 sectors (32×32 parcels). Edge zones are clipped; coordinates remain globally canonical. Never instantiate a full separate world for each zone. Sea/roads/biome/plot world generation uses the immutable shared seed + generator version + content-manifest hash. The coordinator owns assignments, zone epochs, admission and global clock; a zone worker owns living actors and mutable simulation in its zone. A client must never be a source of authoritative position, inventory or persistence state.

### Zone lifecycle and crossing

States: UNLOADED → PREWARM → ACTIVE → DRAINING → UNLOADED; FAILURE transitions to RECOVERING. PREWARM loads deterministic terrain data, shared mesh references, collision and resident overlay; ACTIVE owns ticks; DRAINING rejects new entries and flushes a durable checkpoint before releasing ownership. A player within a configured distance of a boundary causes adjacent zones to prewarm. On crossing, coordinator obtains a new destination lease/epoch, freezes source actor at an authoritative tick, persists a fenced transfer record with sequence and actor revision, acknowledges destination acquisition and only then relinquishes source. Timeout/retry resumes same transfer ID; NEVER create a second actor. If destination fails, restore source authority or keep the actor safely suspended until recovery. Vehicle plus occupants are one transfer group with a stable shared transfer ID, so no passenger or cargo is duplicated.

A **zone lease** is (world ID, zone ID, epoch, lease expiry, worker ID). Every state write and handoff is fenced by epoch; older owner epochs cannot write after reassignment. Movement near boundaries must use destination collision preflight. Read-only ghost entities may appear in adjacent zones for interpolation but cannot take authoritative damage or act. Dynamic subscriptions follow zone vicinity rather than broadcasting every NPC/vehicle to every connection. Rate and byte budgets, priority lanes for own-player state, and backpressure on sector descriptors protect bandwidth.

### Terrain meshes and shared geometry

Separate **procedural source fields** from **visual mesh generation** and **physics/collision**. Stable inputs: (world seed, generator version, biome/parcel coordinates, LOD, mesh schema, asset manifest hash). Use deterministic independently seeded substreams for terrain and decoration, not iteration-order random calls. The mesh key is a content address/hash over inputs; disk/cache stores reusable mesh buffers and a compact per-sector placement list. Geometry uses border samples and a one-cell apron shared between neighbors. Adjacent sectors must share exact edge vertices/height samples; deterministic skirts address mixed LOD seams but must not alter authoritative collision. Render LODs are replaceable derived data, never canonical persistent world state. Immutable generated roads and ruins have shared source asset IDs and instance transforms; dynamic props live in overlays.

Start with explicit square tile heightfield meshes (including coast/water) and fixed boundary samples, then build 3D transitions, cliffs, roads, interiors, navigable mesh and streaming-budget refinement. World coordinates are integers/fixed-grid for cell ownership; floating transforms are local render offsets to avoid jitter across long distances. Client may generate meshes locally but the server provides generator and asset versions, validates positions against its own authoritative collider fields, and refuses mismatched terrain contracts.

### Character identity (The Mechanist integration target)

Use the established **TheMechanist** model as source of architectural principles: stable namespaced provider identity via `UserProfileAuthority` / `GameStorageManager`, canonical `CharacterSaveManager` binding within `SecureServerNetworkingCore`, and recovery through `IndependentHostTurnAuthority` / session ledger. These names are integration references, not assertions that the classes were copied into this repository. Review actual source and licenses/interfaces before extraction.

Distinct keys:
- `account_id`: durable, server-issued or linked-provider subject, not computed from a machine fingerprint.
- `device_id`: locally enrolled device keypair identifier; hardware-backed storage when available. A device-bound key proves possession through a nonce signature, never raw hardware serial/CPU/MAC fingerprint sent to the host.
- `character_id`: immutable UUID assigned and persisted by the realm; one account may own several characters, each scoped to realm/ruleset.
- `session_id`: short-lived server-minted session with rotating resume secret, expiry and generation.
- `zone_authority_epoch`: fencing number for exclusive control.

Hardware dependency is an **optional registered-device trust factor**, not the sole identity or recovery mechanism: hardware changes, repairs, OS reinstall and accessibility needs must not erase ownership. Permit recovery via existing proven account credentials plus device re-enrollment; enforce optional stronger controls for high-risk account actions. Never put device or login secrets in world snapshots or network logs.

Connect → encrypted authenticated transport → prove device/account → lookup canonical account → select/claim character → acquire exclusive generation-fenced character session → load authoritative character revision → choose spawn/handoff → begin replication. No login may mutate canonical character state before credential/session validation. Failed binding rolls back without consuming resume token. Duplicate login is denied or explicitly supersedes the old session only through revocation plus generation increment. On disconnect, the server alone saves position, health, conditions, appearance, progression, inventory/ownership references, last zone and last acknowledged revision; it never trusts a client-uploaded save. Character creation and deletion require authenticated commands, unique IDs, validation and tombstone retention. Sanitized public avatar/name is distinct from private account/device data.

Current `PlayerIdentity` in the Malefic Veil hashes a client-stored bearer secret over plaintext LAN AUTH; **do not expose this mechanism publicly**. Migrate using a versioned identity bridge mapping valid legacy account hashes to account IDs with explicit authenticated recovery. No unauthenticated account-claim-by-hash.

### Persistence and crash recovery

Separate (1) immutable world seed/version/manifest, (2) authoritative character store, (3) zone entity overlays, (4) account/device/session ledger, (5) audit/ban records, and (6) derived mesh cache. Never serialize large deterministic geometry every autosave. Suggested transactional design: SQLite WAL on a single dedicated host initially (one explicit writer with transactions and schema migrations), then server-grade transactional DB if zone workers become remote. Keep large immutable mesh bundles in content-addressed files. All writes identify world, zone, authority epoch, entity ID, revision, and idempotency key. Atomic character transfer transaction records `PREPARED`, `ACQUIRED`, `COMMITTED`; on restart reconcile incomplete entries and select one canonical zone owner. No two zones may claim the same character/vehicle.

Save cadence: transactional important events immediately (character creation, login generation, zone handoff, inventory changes); checkpoint zone entity deltas periodically, plus graceful close. Durable write-ahead log, integrity checks and schema migrations must be tested on actual target filesystem. Maintain timestamped backups with off-machine replication, restoration drills and minimum retention; backup is only meaningful after restore validation. Disk-full and corruption must fail closed for mutations rather than acknowledge an unsaved successful transfer or wipe a world. Add metrics for pending writes, checkpoint age, zone tick lag, I/O latency, backlog bytes, lease recovery and backup freshness.

### Hardware and deployment bands (starting hypotheses, benchmark before sizing)

**Local development:** 4+ physical cores, 16 GB RAM, SSD/NVMe; one process with coordinator, auth and a small number of active zones; loopback networking. **Small dedicated community host (~8–24 concurrent players):** modern 6–8 fast physical CPU cores, 32 GB RAM, 1 TB reliable NVMe with spare capacity, stable wired connection and off-host backup; validate under actual NPC/vehicle load before committing to capacity. **Larger realm:** isolate database, identity and zone workers; capacity scales primarily with active simulation density, AI tick and replication traffic, not simply total generated continent size. Plan redundancy/failover only after exclusive leases, recovery and durable save gates pass. These are provisional sizing envelopes, not measured game requirements.

Secure public deployment requires TLS or properly authenticated encrypted transport, restricted admin plane, per-IP/admission rate limiting, secrets management, firewall, metrics, log rotation, crash restart policy, UPS where practical, and non-root service accounts. Do not deploy the present plaintext LAN protocol on the internet. Prefer regular service managers and direct test scripts; no GitHub Actions requirement.

## Phases and acceptance gates

| Phase | Deliverable | Acceptance |
|---|---|---|
| Z1 | Pure zone coordinate/lease domain and neighbor selection | Every parcel in exactly one zone, deterministic mapping, boundaries and epoch fencing unit tested |
| Z2 | Shared terrain samples, sector mesh cache and LOD seam contract | Same seed/content version yields byte-consistent meshes across fresh processes; adjacent borders match |
| Z3 | Active zone lifecycle with interest subscriptions | Prewarm/load/drain under 2+ clients, no duplication or missing actors, bounded memory/bandwidth |
| Z4 | Authenticated device enrollment + canonical account/character split | Duplicate and forged sessions denied; migrated account recoverable after hardware replacement |
| Z5 | Transactional character and zone store, restart reconciliation | Kill process at every transfer stage; exactly one character owner and no acknowledged state loss |
| Z6 | Drivable cross-zone vehicle group transfer | Driver/passengers/car/cargo transition together under latency and restart; no teleport/duplication |
| Z7 | Dedicated hosting operator toolkit | Backups and restore, health endpoint restricted to localhost, zone metrics, moderation and safe maintenance |
| Z8 | Multiplayer hardening and physical acceptance | 24h soak including restarts, 8–24 concurrent clients as benchmark target, degraded-network tests and manual traversal |

**Strict boundaries:** Construction and weapon work remains in its existing parallel streams. Expose stable world-coordinate, entity-save and character-state interfaces for their later integration without implementing those systems here. Existing `ContinentSectors` and `LanServer` sector-descriptor queries are preliminary G2 primitives, not completed streaming, character authentication or public hosting.
