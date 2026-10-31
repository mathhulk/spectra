/** Spectra wire protocol. A named module so packet hierarchies can be sealed across packages. */
module com.mathhulk.spectra.protocol {
  exports com.mathhulk.spectra.protocol;
  exports com.mathhulk.spectra.protocol.clientbound;
  exports com.mathhulk.spectra.protocol.serverbound;
}
