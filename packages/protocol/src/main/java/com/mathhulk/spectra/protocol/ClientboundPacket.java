package com.mathhulk.spectra.protocol;

import com.mathhulk.spectra.protocol.clientbound.*;

/** A packet sent from the server to the client. */
public sealed interface ClientboundPacket extends Packet
    permits Welcome, ResourceManifest, ResourceChunk, OpenView, SetViewVisible, CloseView, ViewMessage,
    HttpResponse, HttpResponseBody, SocketOpened, SocketMessage, SocketClosed, NetworkPolicy {
}
