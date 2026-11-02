# Security model

Spectra renders code from servers inside the player's game, so the client limits what that code can do.

| Concern | How Spectra handles it |
|---|---|
| A page escaping its server | Each server gets its own origin (`https://<hash>.spectra.invalid/`, derived from a server id rather than the address), so storage and cookies are isolated per server |
| Tracking players or contacting arbitrary hosts | Requests outside the origin are cancelled unless the server [allowlists](/ui/network) them, and players can turn that off |
| Opening things on the player's computer | Navigation is pinned to the origin. External links need the player's confirmation, and only `https` links are allowed |
| Malicious resource paths | Paths are validated on both sides, and the client stores files by hash, so server-chosen names never touch the file system |
| Corrupted or tampered downloads | Every file is checked against its SHA-256 before it is stored |
| Oversized or malformed packets | Every field has a size limit and is bounds-checked. Malformed frames are dropped, and the server disconnects clients that send too many |
| Server messages injected into pages | Messages are delivered as escaped strings and parsed with `JSON.parse` |
| Spoofed view identity | `event.player` and `event.viewId` on the server come from the connection and the browser frame, not from the page |

Things you're responsible for as a server developer:

- **Validate everything views send.** Messages, request bodies, socket data and query strings are player input.
- **Check permissions on the server**, never in the page.
