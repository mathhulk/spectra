package com.mathhulk.spectra;

import com.mathhulk.spectra.ui.*;

import net.fabricmc.api.ClientModInitializer;

public class SpectraClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ServerManager.initialize();
  }
}