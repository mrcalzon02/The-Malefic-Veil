# Dedicated Java 17 host

Install JDK 17+ and Python 3. Set JAVA_HOME to that JDK (on Windows, use its bin before Oracle's launcher aliases). Build from the repository root:

```text
python tools/build_server.py
java -cp build/dedicated-server.jar hbnet.Setup server-network
```

Setup refuses to overwrite an existing server identity. It generates a persistent PKCS12 certificate/key, protected keystore-password file, salted room verifier and generated room secret. `--manual` reads a 12–128 printable ASCII password from an actual secret console prompt, preserving spaces exactly. No password is passed on argv or printed. Read `server-network/server.fingerprint` and, for generated keys, `server-network/room.secret` privately and deliver out of band. The room secret grants no administrator authority. Protect and back up this directory; Windows owner ACLs/POSIX owner permissions are applied. Keys remain exportable by the OS owner and administrators; no hardware/non-exportability promise is made. Identity/password rotation and recovery need operator review; do not delete identity files as an update procedure.

Start (same Java command works in Windows and Linux):

```text
java -Djava.awt.headless=true -Dveil.secure=server-network -Dveil.bind=127.0.0.1 -jar build/dedicated-server.jar 7777 20261020 server-data
```

`server.cmd` / `sh server.sh` pass the same arguments and supply a 1 GiB heap. Override heap flags with the direct Java command. Default binding is loopback. An explicit bind/listener choice is required for remote reachability; no firewall, UPnP, port forwarding, public-address lookup or discovery service is installed. Legacy listeners and clients restrict unauthenticated/plaintext connections to trusted private/link/loopback addresses; a user-managed private reverse proxy or NAT setup still requires operator judgment. Do not advertise this prototype on WAN while security acceptance is failing.

This extends the existing veil.LanServer, NetLink and persistent checkpoint rules. The secure path derives `acct_...` from the proven installation public key before world allocation and rejects the old AUTH command. Existing moderation/allowlist decisions still apply. Old LAN endpoint-derived accounts are not silently reassigned to new secure keys; operator-assisted migration remains unimplemented. Native GPU JOIN now has masked TLS fields, saved servers and local contacts and passes the transport to the existing game; its Maven compilation was checked, but GPU interaction/capture acceptance remains unverified. Native host/solo flows remain on their existing trusted local transport; a selectable secure listen-host GUI is unfinished. Local console `status`, `players`, `save`, `shutdown`, kick/ban/allowlist operations retain their existing meanings. No GPU renderer is required by the host.

For updates: stop through STDIN, verify exit/port release, back up data and the complete identity directory, build the reviewed source, replace the JAR, restart with unchanged identity and data paths, and run acceptance. For rollback retain the previous JAR and full coherent save/identity backup. Malefic keeps its existing validated world/checkpoint/backup migration constraints. No unauthenticated remote RCON or web administrator exists. OS systemd/Windows service wrappers and automatic restart policies are operator-managed and not installed by this change.
