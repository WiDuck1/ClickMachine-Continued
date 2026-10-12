package dev.shadowsoffire.clickmachine.gui;

import java.util.function.BooleanSupplier;

import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClickerCheckboxButton extends AbstractButton {

    private static final Identifier CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final Identifier CHECKBOX_SELECTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected");
    private static final Identifier CHECKBOX_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_highlighted");
    private static final Identifier CHECKBOX_SPRITE = Identifier.withDefaultNamespace("widget/checkbox");

    protected final ClickMachineScreen gui;
    protected final int index;
    protected final BooleanSupplier selected;

    public ClickerCheckboxButton(ClickMachineScreen gui, int x, int y, int width, int height, Component title, int index, BooleanSupplier selected) {
        super(x, y, width, height, title);
        this.gui = gui;
        this.index = index;
        this.selected = selected;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.gui.getMenu().containerId, this.index - 1);
    }

    @Override
    public boolean isFocused() {
        return super.isHovered() || super.isFocused();
    }

    public boolean isSelected() {
        return selected.getAsBoolean();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Identifier sprite;
        if (this.isSelected()) {
            sprite = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
        }
        else {
            sprite = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
        }
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height, this.alpha);
        gfx.text(font, this.getMessage().getString(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 0xFF404040, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

}
