package com.mojang.launcher.game.process;

import java.io.IOException;

public interface GameProcessFactory {

    GameProcess startGame(GameProcessBuilder var1) throws IOException;
}
