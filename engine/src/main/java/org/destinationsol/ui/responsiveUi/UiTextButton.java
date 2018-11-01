/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol.ui.responsiveUi;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import org.destinationsol.SolApplication;
import org.destinationsol.common.SolColor;
import org.destinationsol.ui.DisplayDimensions;
import org.destinationsol.ui.FontSize;
import org.destinationsol.ui.SolInputManager;
import org.destinationsol.ui.UiDrawer;

import java.util.Optional;

@Deprecated
public class UiTextButton extends AbstractUiElement implements UiResizableElement {
    public static final int DEFAULT_BUTTON_WIDTH = 300;
    public static final int DEFAULT_BUTTON_HEIGHT = 75;
    public static final int DEFAULT_BUTTON_PADDING = 10;

    private Rectangle screenArea;

    private String displayName;
    private int triggerKey;
    private boolean isWithSound;
    private boolean isEnabled = true;

    private boolean isKeyPressed;
    private boolean wasKeyPressed;
    private boolean isKeyFlashed;
    private boolean isAreaPressed;
    private boolean isAreaFlashed;
    private boolean isAreaJustUnpressed;
    private boolean doesMouseHover;

    // TODO: Warn probably means highlight in this context. Investigate.
    private int warnCount;
    private Color warnColor = SolColor.WHITE;

    private int x;
    private int y;
    private int width = DEFAULT_BUTTON_WIDTH;
    private int height = DEFAULT_BUTTON_HEIGHT;

    // TODO: Make these optional?
    private UiCallback onClickAction; // Called *while* button is pressed
    private UiCallback onReleaseAction; // Called when button is released
    private boolean wasResized;

    @Override
    public UiTextButton setPosition(int x, int y) {
        this.x = x;
        this.y = y;

        calculateScreenArea();

        return this;
    }

    @Override
    public UiTextButton setParent(UiContainerElement parent) {
        this.parent = Optional.of(parent);
        return this;
    }

    /**
     * Sets the text this button displays.
     *
     * @param displayName Text to display
     * @return Self for method chaining
     */
    public UiTextButton setDisplayName(String displayName) {
        this.displayName = displayName;
        if (!wasResized || width < getMinHeight()) {
            setWidth(getDefaultWidth());
            getParent().ifPresent(UiElement::recalculate);
            wasResized = false;
        }
        return this;
    }

    public UiTextButton setTriggerKey(int triggerKey) {
        this.triggerKey = triggerKey;

        return this;
    }

    public UiTextButton setOnClickAction(UiCallback onClickAction) {
        this.onClickAction = onClickAction;

        return this;
    }

    public UiTextButton setOnReleaseAction(UiCallback onReleaseAction) {
        this.onReleaseAction = onReleaseAction;

        return this;
    }

    public UiTextButton enableSound() {
        this.isWithSound = true;

        return this;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean maybeFlashPressed(int keyCode) {
        if (!isEnabled) {
            return false;
        }

        if (triggerKey == keyCode) {
            isKeyFlashed = true;
            return true;
        }

        // TODO: Not present in original implementation. Examine why it wasn't, and look at consequences of adding it.
        // isKeyFlashed = false;
        return false;
    }

    @Override
    public boolean maybeFlashPressed(SolInputManager.InputPointer inputPointer) {
        if (!isEnabled) {
            return false;
        }

        boolean pressed = screenArea != null && screenArea.contains(inputPointer.x, inputPointer.y);
        if (pressed) {
            isAreaFlashed = true;
        }

        return pressed;
    }

    @Override
    public boolean update(SolInputManager.InputPointer[] inputPointers, boolean cursorShown, boolean canBePressed, SolInputManager inputMan, SolApplication cmp) {
        if (!isEnabled) {
            canBePressed = false;
        }
        updateKeys(canBePressed);
        updateArea(inputPointers, canBePressed);
        updateHover(inputPointers, cursorShown, inputMan, cmp);
        if (isWithSound && isJustOff()) {
            inputMan.playClick(cmp);
        }
        if (warnCount > 0) {
            warnCount--;
        }

        if (isOn()) {
            if (onClickAction != null) {
                onClickAction.callback(this);
            }
        } else if (isJustOff()) {
            if (onReleaseAction != null) {
                onReleaseAction.callback(this);
            }
        }

        return (isOn() || isJustOff());
    }

    private void updateHover(SolInputManager.InputPointer[] inputPointers, boolean cursorShown, SolInputManager inputMan, SolApplication cmp) {
        if (screenArea == null || isAreaPressed || inputPointers[0].pressed) {
            return;
        }
        boolean prev = doesMouseHover;
        doesMouseHover = cursorShown && screenArea.contains(inputPointers[0].x, inputPointers[0].y);
        if (isWithSound && doesMouseHover && !prev) {
            inputMan.playHover(cmp);
        }
    }

    private void updateKeys(boolean canBePressed) {
        wasKeyPressed = isKeyPressed;
        if (isKeyFlashed) {
            isKeyPressed = true;
            isKeyFlashed = false;
        } else {
            if (canBePressed) {
                isKeyPressed = Gdx.input.isKeyPressed(triggerKey);
            }
        }
    }

    private void updateArea(SolInputManager.InputPointer[] inputPointers, boolean canBePressed) {
        if (screenArea == null) {
            return;
        }
        isAreaJustUnpressed = false;
        if (isAreaFlashed) {
            isAreaPressed = true;
            isAreaFlashed = false;
        } else {
            isAreaPressed = false;
            if (canBePressed) {
                for (SolInputManager.InputPointer inputPointer : inputPointers) {
                    if (!screenArea.contains(inputPointer.x, inputPointer.y)) {
                        continue;
                    }
                    isAreaPressed = inputPointer.pressed;
                    isAreaJustUnpressed = !inputPointer.pressed && inputPointer.prevPressed;
                    break;
                }
            }
        }
    }

    // poll to perform continuous actions
    public boolean isOn() {
        return isEnabled && (isKeyPressed || isAreaPressed);
    }

    // poll to perform one-off actions
    public boolean isJustOff() {
        return isEnabled && (!isKeyPressed && wasKeyPressed || isAreaJustUnpressed);
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void draw() {
        if (screenArea == null) {
            return;
        }

        Color tint = SolColor.UI_INACTIVE;
        if (isEnabled) {
            if (isOn()) {
                tint = SolColor.UI_LIGHT;
            } else if (doesMouseHover) {
                tint = SolColor.UI_MED;
            } else {
                tint = SolColor.UI_DARK;
            }
        }

        UiDrawer uiDrawer = SolApplication.getUiDrawer();

        uiDrawer.draw(screenArea, tint);
        if (warnCount > 0) {
            uiDrawer.draw(screenArea, warnColor);
        }

        tint = isEnabled ? SolColor.WHITE : SolColor.G;
        uiDrawer.drawString(displayName, screenArea.x + screenArea.width / 2, screenArea.y + screenArea.height / 2, FontSize.MENU, true, tint);
    }

    @Override
    public void blur() {
        isKeyPressed = false;
        wasKeyPressed = false;
        isAreaPressed = false;
        isAreaJustUnpressed = false;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public Rectangle getScreenArea() {
        return screenArea;
    }

    public boolean isMouseHover() {
        return doesMouseHover;
    }

    public void enableWarn() {
        warnCount = 2;
    }

    private void calculateScreenArea() {
        DisplayDimensions displayDimensions = SolApplication.displayDimensions;
        screenArea = new Rectangle(
                displayDimensions.getFloatWidthForPixelWidth(x - width/2),
                displayDimensions.getFloatHeightForPixelHeight(y - height/2),
                displayDimensions.getFloatWidthForPixelWidth(width),
                displayDimensions.getFloatHeightForPixelHeight(height)
        );
    }

    @Override
    public UiTextButton setWidth(int width) {
        this.width = width;
        wasResized = width != getMinWidth();
        calculateScreenArea();

        return this;
    }

    @Override
    public UiTextButton setHeight(int height) {
        this.height = height;
        calculateScreenArea();

        return this;
    }

    @Override
    public int getMinHeight() {
        return SolApplication.getUiDrawer().getStringHeight(displayName, FontSize.MENU) + DEFAULT_BUTTON_PADDING * 2;
    }

    @Override
    public int getMinWidth() {
        return SolApplication.getUiDrawer().getStringLength(displayName, FontSize.MENU) + DEFAULT_BUTTON_PADDING * 2;
    }

    @Override
    public int getDefaultHeight() {
        return Math.max(getMinHeight(), DEFAULT_BUTTON_HEIGHT);
    }

    @Override
    public int getDefaultWidth() {
        return Math.max(getMinWidth(), DEFAULT_BUTTON_WIDTH);
    }
}