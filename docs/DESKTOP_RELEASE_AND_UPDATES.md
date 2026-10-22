# Desktop release + restart-time updates

Every successful push to main (or manual dispatch) triggers the standalone desktop-release workflow. It compiles Java 17, executes the selected headless gameplay and economy gates, and *only after success* creates an immutable `dev-<12 character commit>` GitHub Release as the latest release. The push-triggered packaging includes repository-curated models and game assets.

Download the `MaleficVeil-desktop-<commit>.zip` asset from the latest [Releases page](https://github.com/mrcalzon02/The-Malefic-Veil/releases/latest), extract into a directory, and launch with `play-release.bat` (Windows) or `./play-release.sh` (Unix). Java 17 or newer must be installed. No Android signing key or platform-specific signing is required. Release downloads and upgrades require an internet connection; an unavailable server leaves the existing install available.

The launch scripts run a separate `updater.jar` before `MaleficVeil.jar`. The updater fetches `update.properties` from the latest release, compares the installed SHA with the release SHA, downloads a pinned, immutable `MaleficVeil-<commit>.zip`, verifies its SHA-256, rejects unexpected archive paths and traversal, and extracts to the installation directory. Player save-data paths and launch scripts are excluded from in-place update payloads.

Do not manually treat GitHub upload success as proof of a runnable release. A successful workflow run and retrieval/read-back of the actual release assets are the evidence gate. No signing is needed, but SHA-256 is a corruption and tampering check against the release metadata, **not** a cryptographic signature independent of GitHub. The updater does not run asynchronously inside gameplay.

Outstanding deployment hardening: atomic whole-release rollback and multi-file crash recovery, automatic updater bootstrap replacement, signed provenance/independent trust pinning, UI progress/errors and settings for automatic checks. The current updater extracts files one by one after verification; interruption partway through can leave mixed versions. Until this is addressed, the workflow does not meet production-grade rollback requirements.
