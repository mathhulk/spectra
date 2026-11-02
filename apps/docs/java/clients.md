# Clients

```java
Clients clients = Spectra.get().clients();

ClientInfo now = clients.info(uuid);                    // PENDING until the handshake resolves
clients.resolve(uuid).thenAccept(info -> { ... });      // completes once known
Registration r = clients.onResolved(info -> { ... });   // every player, as they resolve
```

`ClientInfo` is a record with these members:

- `player()`
- `status()`: `PENDING`, `SUPPORTED`, `UNSUPPORTED` or `INCOMPATIBLE`
- `protocolVersion()`
- `modVersion()`
- `capabilities()`
- `isResolved()` and `isSupported()`
- `has(capability)`

Capability constants live in `com.mathhulk.spectra.api.clients.Capability`: `RESOURCES`, `SCREENS`, `LAYERS`, `MESSAGES`, `HTTP`, `SOCKETS`, `NETWORK`.

Spectra never kicks players on a plugin's behalf. Enforce whatever your feature needs. See [Client capabilities](/ui/capabilities).
