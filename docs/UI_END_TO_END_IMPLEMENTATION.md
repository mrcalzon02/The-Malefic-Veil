# End-to-end UI implementation gates

The Swing client now includes a compact menu bar with Player, World, Vehicles, Production and Settings menus. These expose real inventory/skills, existing field crafting, maps, crew administration, enter/exit, load/recover cargo, graphics settings, and read-only live vehicle and industrial recipe inspection. The active truck HUD displays speed, fuel, hull integrity and cargo. The menu bar is not proof that unimplemented transactions work.

Required complete flow:
1. Boot -> clear main menu -> solo / host / join -> options -> faction and class -> game loading -> playable SAFE spawn.
2. Play -> inventory equipment and drop/transfer -> look-targeted container open, contents, take-all, stack splitting -> crafting with ingredient availability and station identity.
3. Settlement -> claim/permissions -> base level and facility slots -> storage limits -> power, munitions, faction-specific sustenance reserves -> construction preview and cost -> automation control.
4. Workshops/factories -> show eligible recipes, production queue, inputs, output capacity, utility, wear, skill influence -> host-authoritative submit/cancel/retrieve.
5. Vehicles -> vehicle-specific driving dashboard -> park/dismount -> outside repair panel with component status and real repair-material debit -> facility assembly queue, parts, resource cost and deployment -> recovered world vehicles or crafted vehicles.
6. Combat manufacturing -> weapons, ammunition, component recipes, faction legality, actual ammo magazines and reload inventories; food/water and other survival supplies must be covered by compatible stations.

QoL hard gates: compact screens, keyboard navigation and remapping, clear reasons for blocked actions, no inert buttons, server-authoritative costs, no double-spend, no accidental loss on full inventory, all flows recover after disconnect, protection against spawn-route obstruction. Do not present a menu as a functioning system if corresponding host services are missing.

Current limitations: no playable GUI container inventory, vehicle repair components, vehicle assembly factory/deployment, industrial work-order frontend, or integrated ammo production. These require actual gameplay transactions and tests, not mock panels. Java compile, headless desktop and physical multi-client playtests remain unverified for this increment.
