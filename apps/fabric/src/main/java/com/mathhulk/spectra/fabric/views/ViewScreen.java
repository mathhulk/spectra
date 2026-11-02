package com.mathhulk.spectra.fabric.views;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/** A full-screen view. Escape closes it and tells the server; server-initiated closes do not echo back. */
final class ViewScreen extends Screen {
  private final BrowserView view;
  private final ViewManager manager;
  private boolean closedByServer;

  ViewScreen(BrowserView view, ViewManager manager) {
    super(Component.empty());
    this.view = view;
    this.manager = manager;
  }

  BrowserView view() {
    return view;
  }

  void markClosedByServer() {
    closedByServer = true;
  }

  @Override
  protected void init() {
    view.resize(width, height);
    view.browser().setFocus(true);
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    view.resize(width, height);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void removed() {
    view.browser().setFocus(false);
    manager.screenRemoved(view, closedByServer);
    super.removed();
  }

  @Override
  public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    if (view.visible()) {
      view.draw(graphics, 0, 0, width, height);
    }
  }

  private int scaled(double value) {
    return (int) (value * minecraft.getWindow().getGuiScale());
  }

  @Override
  public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
    view.browser().setFocus(true);
    view.browser().sendMousePress(scaled(event.x()), scaled(event.y()), event.button());
    return true;
  }

  @Override
  public boolean mouseReleased(@NonNull MouseButtonEvent event) {
    view.browser().sendMouseRelease(scaled(event.x()), scaled(event.y()), event.button());
    return true;
  }

  @Override
  public void mouseMoved(double mouseX, double mouseY) {
    view.browser().sendMouseMove(scaled(mouseX), scaled(mouseY));
  }

  @Override
  public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
    view.browser().sendMouseMove(scaled(event.x()), scaled(event.y()));
    return true;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    view.browser().sendMouseWheel(scaled(mouseX), scaled(mouseY), scrollY, 0);
    return true;
  }

  @Override
  public boolean keyPressed(@NonNull KeyEvent event) {
    if (event.isEscape() && shouldCloseOnEsc()) {
      onClose();
      return true;
    }
    view.browser().sendKeyPress(event);
    return true;
  }

  @Override
  public boolean keyReleased(@NonNull KeyEvent event) {
    view.browser().sendKeyRelease(event);
    return true;
  }

  @Override
  public boolean charTyped(@NonNull CharacterEvent event) {
    if (event.codepoint() == 0) {
      return false;
    }
    for (char c : Character.toChars(event.codepoint())) {
      view.browser().sendKeyTyped(c, 0);
    }
    return true;
  }
}
