package com.miloshpetrov.sol2;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.miloshpetrov.sol2.common.Col;
import com.miloshpetrov.sol2.common.SolMath;
import com.miloshpetrov.sol2.game.*;
import com.miloshpetrov.sol2.menu.GameOptions;
import com.miloshpetrov.sol2.menu.MenuScreens;
import com.miloshpetrov.sol2.ui.*;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SolCmp {

  private final SolInputMan myInputMan;
  private final UiDrawer myUiDrawer;
  private final MenuScreens myMenuScreens;
  private final TexMan myTexMan;
  private final SolLayouts myLayouts;
  private final boolean myReallyMobile;
  private final GameOptions myOptions;
  private final CommonDrawer myCommonDrawer;

  private String myFatalErrorMsg;
  private String myFatalErrorTrace;

  private float myAccum = 0;
  private SolGame myGame;

  public SolCmp() {
    myReallyMobile = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;
    if (myReallyMobile) DebugOptions.read(true);
    myOptions = new GameOptions(true, isMobile());

    myTexMan = new TexMan();
    myCommonDrawer = new CommonDrawer();
    myUiDrawer = new UiDrawer(myTexMan, myCommonDrawer);
    myInputMan = new SolInputMan(myTexMan, myUiDrawer.r);
    myLayouts = new SolLayouts(myUiDrawer.r);
    myMenuScreens = new MenuScreens(myLayouts, myTexMan, isMobile(), myUiDrawer.r);

    myInputMan.setScreen(this, myMenuScreens.main);
  }

  public void render() {
    myAccum += Gdx.graphics.getDeltaTime();
    while (myAccum > Const.REAL_TIME_STEP) {
      safeUpdate();
      myAccum -= Const.REAL_TIME_STEP;
    }
    draw();
  }

  private void safeUpdate() {
    if (myFatalErrorMsg != null) return;
    try {
      update();
    } catch (Throwable t) {
      t.printStackTrace();
      myFatalErrorMsg = "A fatal error occurred:\n" + t.getMessage();
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      myFatalErrorTrace = sw.toString();
    }
  }

  private void update() {
    DebugCollector.update();
    DebugCollector.debug("Fps", Gdx.graphics.getFramesPerSecond());
    myInputMan.update(this);
    if (myGame == null) {
      DebugCollector.debug("Version", Const.VERSION);
    } else {
      myGame.update();
    }
    SolMath.checkVectorsTaken(null);
  }

  private void draw() {
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    myCommonDrawer.begin();
    if (myGame != null) {
      myGame.draw();
    }
    myUiDrawer.updateMtx();
    myInputMan.draw(myUiDrawer, this);
    if (myGame != null) {
      myGame.drawDebugUi(myUiDrawer);
    }
    if (myFatalErrorMsg != null) {
      myUiDrawer.draw(myUiDrawer.whiteTex, myUiDrawer.r, .5f, 0, 0, 0, .25f, 0, Col.UI_BG);
      myUiDrawer.drawString(myFatalErrorMsg, myUiDrawer.r / 2, .5f, FontSize.MENU, true, Col.W);
      myUiDrawer.drawString(myFatalErrorTrace, .2f * myUiDrawer.r, .6f, FontSize.DEBUG, false, Col.W);
    }
    DebugCollector.draw(myUiDrawer);
    myCommonDrawer.end();
  }

  public void startNewGame(boolean tut, boolean usePrevShip) {
    if (myGame != null) throw new AssertionError();
    myGame = new SolGame(this, usePrevShip, myTexMan, tut, myCommonDrawer);
    myInputMan.setScreen(this, myGame.getScreens().mainScreen);
  }

  public SolInputMan getInputMan() {
    return myInputMan;
  }

  public MenuScreens getMenuScreens() {
    return myMenuScreens;
  }

  public void dispose() {
    myCommonDrawer.dispose();
    if (myGame != null) myGame.onGameEnd();
    myTexMan.dispose();
    myInputMan.dispose();
  }

  public SolGame getGame() {
    return myGame;
  }

  public SolLayouts getLayouts() {
    return myLayouts;
  }

  public void finishGame() {
    myGame.onGameEnd();
    myGame = null;
    myInputMan.setScreen(this, myMenuScreens.main);
  }

  public TexMan getTexMan() {
    return myTexMan;
  }

  public boolean isMobile() {
    return DebugOptions.EMULATE_MOBILE || myReallyMobile;
  }

  public GameOptions getOptions() {
    return myOptions;
  }
}
