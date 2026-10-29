# World creation and loading

The desktop launcher now has distinct Single Player, Host Multiplayer, and Join Multiplayer paths. The first two offer Create World and Load World; Joining prompts for the remote address and port and must never rewrite the host's configuration.

Creation prompts for a named world, numeric or random seed, normal/hardcore character mode, AI population (0-24), host player capacity, and reserved enemy-density / resource-abundance settings. The requested baseline is **New Settlement**, random seed, normal mode, solo host, normal enemies, normal resources, one player, six AI characters. Names and options are validated before a new `worlds/<world_name>/world.properties` record is written. Creating an existing name is rejected. Loading chooses a saved world, not a newly randomized seed.

**Solo mode now uses an embedded `LanServer` on an ephemeral loopback port** and `NetLink`, instead of a separate UI-only local simulation. Both embedded solo and local-host multiplayer pass their save directory to the authoritative server for realm autosaves and account persistence. Saved seed, hardcore choice and AI count are checked before server construction. The saved maximum connected player count configures the host connection semaphore. The game still binds multi-hosting to the existing LAN address and port 7777; this is not an Internet security release.

**Limitation:** Enemy density and loot abundance are stored and strictly validated, but the underlying world-generation and resource directors currently retain their default balance. The creation dialog labels these as reserved; choosing different values does not yet alter gameplay and must not be advertised as implemented difficulty balancing. Character progression and campaign save/load require gameplay verification. Live desktop build, multiplayer hosting, and restart recovery are not independently tested.

Source `WorldCreationConfig.java`; basic checks `WorldCreationConfigTest.java`.
