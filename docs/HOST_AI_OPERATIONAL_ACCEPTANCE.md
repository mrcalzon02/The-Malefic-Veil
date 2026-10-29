# Host AI — real-game operational acceptance

## Verified in the Java 17 authoritative host

Host AI is a host setting, **not** a decorative client actor or a second world. Supported configurations are 0–24 bots; `BALANCED` distributes round-robin among Unraveled, Penitent and Begotten; `SINGLE_FACTION` puts all on faction 1, 2 or 3; `CUSTOM` uses an explicit three-part count. Each bot starts at requested level 1–50, keeps an authenticated deterministic bot account and faction, and joins a real FactionGroups crew of **no more than three**. Separate crews of the same faction remain allied.

Host LAN displays a compact bot setup dialog. Dedicated host CLI examples:

```sh
java -cp build/classes veil.LanServer 7777 20261021 server-data --ai-count=5 --ai-mode=BALANCED --ai-level=7
java -cp build/classes veil.LanServer 7777 20261021 server-data --ai-count=6 --ai-mode=SINGLE_FACTION --ai-team=3 --ai-level=5
java -cp build/classes veil.LanServer 7777 20261021 server-data --ai-count=6 --ai-mode=CUSTOM --ai-split=1,2,3 --ai-level=10
```

(The actual launch scripts determine the runtime classpath; these examples specify the application class and AI options, not a promise that `build/classes` exists in every distribution.)

At runtime host console `ai` now reports faction, builder/provisioner/sentinel duty, current activity, level, base count, carried mass, crew and world-coordinate position. These values come from actual server actors. Clients receive **eight-field** `AI_ACTOR` messages matching the server's packet exactly (including duty), plus ordinary replicated `PEER` positions. This protocol mismatch was found and fixed during acceptance.

Bots explore one continuous world, pursue reachable natural resource nodes using the same speed/collision and ray-based harvesting as players, consume real food and ammunition, hand-craft tent/campfire/crate kits with actual inventory debit, place actual owner-keyed `CAMP_STAKE` and turret devices where affordable, react to opponents with host combat, and give physical inventory to nearby crew leaders. Members of a crew deploy close enough to meet and transfer goods instead of independently materializing on distant regions. None of these actions grants artificial stock or teleport mobility.

The full `QualityGateTest` (see `FactionAiDirectorTest`, `FactionAiTacticsTest`, `FactionAiCooperationTest`, and `HostAiNetworkTest`) now checks example populations 5 balanced and 6 Begotten, configured levels, strict crew cap, real actor positions, actual resource depletion, owned field-camp construction, survival inventory debit and transfer, allied combat rules, load/save inventory checks, TCP metadata replication and duty. The [verified passing host pipeline](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39719666713) passed on Windows and Ubuntu with both JDK 17 and JDK 21. The full new host state should be rechecked for each subsequent source change.

## Important remaining gameplay boundaries

**Do not confuse early autonomous survival camps with the full level-50 industrial settlement economy.** Bots currently build **field camps and limited defensive turrets**, not all 28 kinds of facilities, all 50 upgrade tiers, complete power, water, farm, manufacturing, trade logistics or faction-specialized tech chains. `SettlementAuthority` has strong founder-character ownership/capacity *policy*, and `SettlementSupplyBalance` specifies external production dependence, but those are not yet integrated with all physical world facilities. Three co-operating characters can begin distinct bases and move real supplies; a self-maintaining maximum-defense level-50 three-base industrial network is not yet implemented.

An existing dedicated realm's AI roster/faction/starting-level settings are now immutable without a deliberate migration. `HostAiSettings.bind` validates the saved rules **before** reconstructing the bot accounts. This protects owner keys, faction alliances and crews from silent redistribution. The current ordinary LAN host dialog uses a transient host (null save directory); durable realm restarts and account journals apply to dedicated hosts with an explicit save directory.

Next release gate: integrate persistent physical production/storage buildings with `SettlementAuthority`, authentic multi-stage faction recipes, work queues, supply convoys and equipment repairs, then let AI transition from field-camp survival to industrial upkeep and defend/raid systems. Do not claim the full economy is complete until transactions, structure identity and distributed resource flow are tested end-to-end.
