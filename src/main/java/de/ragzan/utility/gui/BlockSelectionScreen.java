package de.ragzan.utility.gui;

import de.ragzan.utility.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

public final class BlockSelectionScreen extends Screen {
    private final Screen parent;
    private final boolean forBlockEsp;
    private final ModConfig c = ModConfig.INSTANCE;
    private final List<Block> allBlocks;
    private int page = 0;
    private static final int PAGE_SIZE = 12;

    public BlockSelectionScreen(Screen parent, boolean forBlockEsp) {
        super(Text.literal("Block-Auswahl"));
        this.parent = parent;
        this.forBlockEsp = forBlockEsp;
        this.allBlocks = Registries.BLOCK.stream().sorted(Comparator.comparing(b -> Registries.BLOCK.getId(b).toString())).toList();
    }

    private java.util.Set<String> selection() { return forBlockEsp ? c.blockEspBlocks : c.stashBlocks; }

    @Override
    protected void init() {
        clearChildren();
        int x = width / 2 - 160;
        int y = 44;
        int pages = Math.max(1, (allBlocks.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = page * PAGE_SIZE; i < Math.min(allBlocks.size(), (page + 1) * PAGE_SIZE); i++) {
            Block block = allBlocks.get(i);
            String id = Registries.BLOCK.getId(block).toString();
            boolean enabled = selection().contains(id);
            String label = (enabled ? "✓ " : "□ ") + id;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                if (!selection().remove(id)) selection().add(id);
                c.save();
                b.setMessage(Text.literal((selection().contains(id) ? "✓ " : "□ ") + id));
            }).dimensions(x, y, 320, 19).build());
            y += 21;
        }
        int navY = height - 53;
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> { page = Math.max(0, page - 1); init(); }).dimensions(x, navY, 48, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Seite " + (page + 1) + "/" + pages + " · " + allBlocks.size() + " Blöcke"), b -> {}).dimensions(x + 54, navY, 210, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> { page = Math.min(pages - 1, page + 1); init(); }).dimensions(x + 270, navY, 50, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Alle auswählen"), b -> { for (Block block : allBlocks) selection().add(Registries.BLOCK.getId(block).toString()); c.save(); init(); }).dimensions(x, navY - 27, 155, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Auswahl löschen"), b -> { selection().clear(); c.save(); init(); }).dimensions(x + 165, navY - 27, 155, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent)).dimensions(x, height - 27, 320, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(forBlockEsp ? "BLOCK ESP – alle Minecraft-Blöcke" : "STASH ESP – Lagerblöcke auswählen"), width / 2, 18, 0xFF7CEBFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Anklicken = Auswahl umschalten · Auswahl wird sofort gespeichert"), width / 2, 31, 0xFFCCCCCC);
        super.render(context, mouseX, mouseY, delta);
    }
}
