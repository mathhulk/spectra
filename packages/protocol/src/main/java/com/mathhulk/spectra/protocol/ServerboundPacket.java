package com.mathhulk.spectra.protocol;

import com.mathhulk.spectra.protocol.serverbound.*;

/** A packet sent from the client to the server. */
public sealed interface ServerboundPacket extends Packet
    permits Hello, ResourceRequest, ResourcesSynced, ViewPost, ViewEvent {
}
