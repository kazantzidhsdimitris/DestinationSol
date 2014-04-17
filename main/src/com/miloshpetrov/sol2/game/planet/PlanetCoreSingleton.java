package com.miloshpetrov.sol2.game.planet;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.miloshpetrov.sol2.TexMan;
import com.miloshpetrov.sol2.common.Col;
import com.miloshpetrov.sol2.game.*;

public class PlanetCoreSingleton {
  private final TextureAtlas.AtlasRegion myTex;

  public PlanetCoreSingleton(TexMan texMan) {
    myTex = texMan.getTex("misc/planetCore", null);
  }


  public void draw(SolGame game, Drawer drawer) {
    SolCam cam = game.getCam();
    Vector2 camPos = cam.getPos();
    Planet p = game.getPlanetMan().getNearestPlanet();
    Vector2 pPos = p.getPos();
    float toCamLen = camPos.dst(pPos);
    float vd = cam.getViewDist();
    float gh = p.getMinGroundHeight();
    if (toCamLen < gh + vd) {
      float sz = gh;
      drawer.draw(myTex, sz *2, sz *2, sz, sz, pPos.x, pPos.y, 0, Col.W);
    }
  }
}
