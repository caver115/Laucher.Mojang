package net.minecraft.launcher.ui.tabs.website;

import java.awt.Component;
import java.awt.Dimension;

public interface Browser {

    void loadUrl(String var1);

    Component getComponent();

    void resize(Dimension var1);
}
