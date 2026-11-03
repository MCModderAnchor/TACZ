Editing the TACZ default gun pack

To preserve local edits, set DefaultPackDebug=true in the [gunpack] section of
<game directory>/tacz/tacz-pre.toml. When false, changes to the TACZ version or
bundled resources trigger replacement at startup.

To regenerate, set DefaultPackDebug=false, delete the exported folder
<game directory>/tacz/tacz_default_gun, then restart.
Backups are stored in <game directory>/tacz_backup.
