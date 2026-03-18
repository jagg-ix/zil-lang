# SSHX11 TLM Transaction Catalog (ZIL)

This catalog proposes a concrete transaction vocabulary for modeling SSHX11 with
ZIL TLM layers.

It is designed to align with existing examples:

- `examples/sshx11-vpn-system.zc`
- `examples/sshx11-extension-vscode.zc`
- `examples/sshx11-user-service-api.zc`
- `examples/tlm-domain-macros.zc`
- `examples/tlm-formal-bridge.zc`

## Modeling scope

Use this catalog to represent:

1. control-plane setup and trust establishment,
2. terminal/X11 interactive data exchange,
3. file/sync plane operations (WebDAV-like flows),
4. overlay/routing setup (VPN + reverse SOCKS),
5. recovery/control handoff events.

## Transaction catalog (20)

Each transaction entry includes:

- command identifier (`tx_*`),
- logical source and destination,
- primary payload shape,
- suggested TLM timing level (`loosely_timed` or `approximately_timed`),
- expected status progression (`issued`, `accepted`, optionally `failed`).

| # | Command | Source -> Destination | Payload (abstract) | Timing |
|---|---|---|---|---|
| 1 | `tx_control_connect` | `local_workstation -> ssh_tunnel` | endpoint, port, transport profile | `loosely_timed` |
| 2 | `tx_host_key_verify` | `ssh_tunnel -> control_plane` | host fingerprint, trust policy id | `loosely_timed` |
| 3 | `tx_user_auth` | `local_workstation -> ssh_tunnel` | auth method, actor id, token ref | `loosely_timed` |
| 4 | `tx_session_channel_open` | `control_plane -> data_plane` | session id, channel set | `loosely_timed` |
| 5 | `tx_x11_forward_enable` | `control_plane -> x11_proxy` | display id, forward mode | `loosely_timed` |
| 6 | `tx_xauth_cookie_sync` | `x11_proxy -> control_plane` | cookie id, cookie scope | `loosely_timed` |
| 7 | `tx_keepalive_ping` | `control_plane -> ssh_tunnel` | sequence, ttl, timeout hint | `loosely_timed` |
| 8 | `tx_terminal_input` | `operator -> websocket_bridge` | key/input frame | `approximately_timed` |
| 9 | `tx_terminal_output` | `websocket_bridge -> operator` | output frame chunk | `approximately_timed` |
| 10 | `tx_x11_frame_push` | `x11_proxy -> websocket_bridge` | frame delta, region bounds | `approximately_timed` |
| 11 | `tx_x11_event_pull` | `websocket_bridge -> x11_proxy` | pointer/key event batch | `approximately_timed` |
| 12 | `tx_clipboard_push` | `operator -> data_plane` | mime type, byte length, checksum | `approximately_timed` |
| 13 | `tx_clipboard_pull` | `data_plane -> operator` | mime type, byte length, checksum | `approximately_timed` |
| 14 | `tx_webdav_list` | `vscode_sshx11_extension -> webdav_gateway` | path, filter, depth | `loosely_timed` |
| 15 | `tx_webdav_chunk_write` | `vscode_sshx11_extension -> webdav_gateway` | path, offset, chunk hash | `approximately_timed` |
| 16 | `tx_webdav_chunk_read` | `webdav_gateway -> vscode_sshx11_extension` | path, offset, chunk hash | `approximately_timed` |
| 17 | `tx_vpn_route_program` | `vpn_overlay -> ssh_tunnel` | route table delta, mtu profile | `loosely_timed` |
| 18 | `tx_reverse_socks_open` | `reverse_socks -> ssh_tunnel` | bind address, relay policy | `loosely_timed` |
| 19 | `tx_control_takeover` | `observer -> control_plane` | request id, role target | `loosely_timed` |
| 20 | `tx_recovery_reconnect` | `degraded_node -> control_plane` | cause code, retry budget | `loosely_timed` |

## Mapping to existing SSHX11 LTS events

Suggested mapping to currently modeled events:

- `tx_control_connect` -> `ssh_login`
- `tx_xauth_cookie_sync` -> `prepare_cookies`
- `tx_session_channel_open` -> `websocket_online`
- `tx_vpn_route_program` -> `set_l3_ready`
- `tx_reverse_socks_open` -> `reverse_socks_start`
- `tx_recovery_reconnect` -> `recover_transport`

From extension/user-service models:

- `tx_control_connect` -> `service_start`
- `tx_reverse_socks_open` -> `reverse_socks_start`
- `tx_recovery_reconnect` -> `recover`

## Mapping to existing policy guards

Use these as guard conditions before transaction acceptance:

- `relay_gate`:
  - required for `tx_terminal_input`, `tx_terminal_output`, `tx_x11_frame_push`, `tx_x11_event_pull`.
- `buffer_sync_gate`:
  - required for high-rate streaming transactions (`tx_x11_frame_push`, chunked terminal frames).
- `reverse_socks_gate`:
  - required for `tx_reverse_socks_open`.
- `webdav_gate`:
  - required for `tx_webdav_list`, `tx_webdav_chunk_write`, `tx_webdav_chunk_read`.
- `api_contract_sync`, `per_user_isolation`:
  - required for `tx_control_takeover`, local daemon API-facing commands.

## Minimal ZIL TLM encoding pattern

Use `examples/tlm-domain-macros.zc` conventions:

```zc
// conceptual pattern (macros defined in same file)
USE TLM_TRANSACTION(tx_terminal_input_001, terminal_input, pty_0, key_batch, 128, high, approximately_timed).
USE TLM_SEND(tx_terminal_input_001, operator_client, ws_main).
USE TLM_ACCEPT(tx_terminal_input_001, websocket_bridge).
```

Status chain:

1. `issued` when send is created,
2. `accepted` when guard conditions and route checks pass,
3. optional `failed` + retry via `tx_recovery_reconnect`.

## Suggested next modeling increments

1. Add a dedicated `LTS_ATOM` for control handoff (`observer`, `controller`, `granted`, `revoked`).
2. Add `POLICY` constraints for latency/jitter envelopes per transaction class.
3. Add transaction-class metrics (`interactive`, `bulk`, `control`) with datasource ingest.
