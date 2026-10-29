# Multiplayer test receipts — 2026-10-28

Source base: `df1e7222d57c47b053357caed86487c2ad58aeef`. Platform: Windows, Oracle Java 17.0.12 targeting `--release 17`. Local implementation SHA and remote read-back are recorded below after publication. These are source-development results, not production release acceptance.

Final test invocations: **37 passed, 2 failed**. Compilation/package command invocations: 1 passed, 0 failed. Repeated manifest entries count as separate invocations. Exact argv, duration, exit code and local log hash are in [delivery.json](receipts/delivery.json); [output](receipts/delivery.log) retains failures. Full-suite failures are not waived. The `local` hbnet invocation deliberately does not run socket acceptance; its 39 local cases are independent results.

The failed tests are:
- `hbnet.HbnetTest`: exit 1, 2.5 s. The strict pin is rejected before application credentials; this environment presents a changed certificate. Actual encrypted gameplay remains unverified.
- `veil.SecureLanIntegrationTest`: exit 1, 1.078 s. The strict pin is rejected before application credentials; this environment presents a changed certificate. Actual encrypted gameplay remains unverified.

Total measured command duration: 40.718 s. This sums child-command elapsed times; concurrent work and setup are not included.

Reproduce from the repository root with JAVA_HOME set to JDK 17:

```text
python tools/verify_hbnet.py
python tools/build_server.py
python tools/network_checks.py veil --label acceptance --full
```

The existing Maven GPU build passed (`mvn -B -ntp -f gpu-client/pom.xml -DskipTests package`), which establishes compilation/packaging only. The existing GGBot archive dependency was restored from the approved upstream archive already downloaded by Beckenburg and verified as SHA-256 `db67b0b0fbaa02454a5d000dc9ec1cc53f360e8f41ab44f3fb1c7e8f71e699e4`; it is ignored build input, not committed art. Existing release-manifest gameplay, crafting, ownership, persistence and legacy socket tests were run.

A separate server JVM and two independent client JVMs passed gameplay observations, clean STDIN stop and port-release verification: [process receipt](receipts/processes.json). These runs explicitly used trusted unencrypted loopback; they do not demonstrate TLS or physical LAN. Reproduce with `python tools/process_checks.py veil` after compilation.

## Build/start commands

[Dedicated server instructions](DEDICATED_SERVER.md) give exact Windows/Linux build/start/config commands. No secrets are present in those commands or receipts. `server.cmd` and `server.sh` are committed; standalone JARs were built locally without a GPU/model exporter.

## Not verified / remaining gates

Physical Windows↔Windows/Linux Ethernet/Wi-Fi; NAT-forwarded separate-network WAN; Android↔desktop and API-26/29 TLS/IME behavior; long soak and OS packet impairment; full concurrent adversarial authentication, ACL/invite-revocation and reconnect-theft/race matrix; all Stream Safe loading/diagnostic/crash/overlay/screenshot transitions; clean-machine installation and service-manager lifecycle. The certificate-interception observation is an environment diagnosis, not proof that no further TLS/game bugs exist. Run the unchanged tests on a network presenting the configured certificate and repair any failures. Detailed per-feature gaps are in [LOCAL_FIRST_MULTIPLAYER.md](LOCAL_FIRST_MULTIPLAYER.md).

## Artifact hashes

- `delivery.json` SHA-256 `b53a71df7e6f56433bc899fe8f9c42a38d0b23ed1c05c561afbdf92b9c1f144d`
- `delivery.log` SHA-256 `b76a84da0ab49146794902d897a01def372ae6676a9b27edc279650fb6e82f72`
- `processes.json` SHA-256 `ceb74bd78ec69bc84d4ed661582ba534327109f0849dac422cb6f20a87698801`
- `gpu-build-final.log` SHA-256 `218d331b0027ebd69c56b54263fd87e02ae5669fdb70f4948107ee339d8cd72c`

## Verification after concurrent-main reconciliation

Base after preserving newer main: `18abee13b4aa67ee6bc4ba0e4aa1a99fd95638b4`. Recompiled and reran the affected suites: **39 test invocations passed, 2 failed**, with unchanged strict certificate-pin failures. Exact commands/durations are in [reconciled-final.json](receipts/reconciled-final.json). No force push was used.

The reconciled native GPU Maven build also passed: [log](receipts/gpu-reconciled.log).


## Exact-byte evidence check

- `delivery.json` SHA-256 `b53a71df7e6f56433bc899fe8f9c42a38d0b23ed1c05c561afbdf92b9c1f144d`
- `delivery.log` SHA-256 `b76a84da0ab49146794902d897a01def372ae6676a9b27edc279650fb6e82f72`
- `gpu-build-final.log` SHA-256 `218d331b0027ebd69c56b54263fd87e02ae5669fdb70f4948107ee339d8cd72c`
- `gpu-reconciled.log` SHA-256 `b445ea03fec7e042248f365e89930e93fb9f32656cbffcd65ebbbef3bb65273c`
- `processes.json` SHA-256 `ceb74bd78ec69bc84d4ed661582ba534327109f0849dac422cb6f20a87698801`
- `reconciled-final.json` SHA-256 `3248bb7bcf5ac111b1b5e45a04557b6f06c8cbb907b6e732e049da6c02c984b7`
- `reconciled-final.log` SHA-256 `9a179dc44f2c04e53a5be7d0681f7a08ccf8b27ae6e50aeeaea863453f7ccd4c`

Source parity uses UTF-8/LF Git-blob canonicalization so Windows checkout line endings cannot create false source drift; all three copies were compared and the checksummed verifier passed.
