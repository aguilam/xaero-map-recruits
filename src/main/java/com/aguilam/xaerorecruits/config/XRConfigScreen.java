package com.aguilam.xaerorecruits.config;

import com.aguilam.xaerorecruits.XaeroRecruitsMod;
import com.aguilam.xaerorecruits.claims.XaeroRefresher;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class XRConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final long SAVE_DEBOUNCE_MS = 250L;

    private final Screen parent;

    private boolean showClaims;
    private boolean showOnMinimap;
    private boolean showMinimapInfo;
    private boolean replaceRecruitsMap;
    private double worldMapLabelScale;
    private double minimapLabelScale;
    private double minimapLabelMaxZoom;
    private double worldMapLabelMinScale;
    private double worldMapLabelMaxScale;

    private boolean dirty;
    private long lastChangeAt;

    public XRConfigScreen(Screen parent) {
        super(Component.translatable("xaerorecruits.config.title"));
        this.parent = parent;

        XRConfig.Client config = XRConfig.CLIENT;
        this.showClaims = config.showClaims.get();
        this.showOnMinimap = config.showOnMinimap.get();
        this.showMinimapInfo = config.showMinimapInfo.get();
        this.replaceRecruitsMap = config.replaceRecruitsMap.get();
        this.worldMapLabelScale = config.worldMapLabelScale.get();
        this.minimapLabelScale = config.minimapLabelScale.get();
        this.minimapLabelMaxZoom = config.minimapLabelMaxZoom.get();
        this.worldMapLabelMinScale = config.worldMapLabelMinScale.get();
        this.worldMapLabelMaxScale = config.worldMapLabelMaxScale.get();
    }

    @Override
    protected void init() {
        int gap = 10;
        int columnWidth = Math.max(120, Math.min(220, (this.width - 50) / 2));
        int left = this.width / 2 - columnWidth - gap / 2;
        int right = this.width / 2 + gap / 2;
        int startY = 42;

        this.addToggle("xaerorecruits.config.show_claims", () -> this.showClaims,
                this::setShowClaims, left, startY, columnWidth);
        this.addSlider("xaerorecruits.config.minimap_label_max_zoom", 0.0D, 10.0D, 0.5D,
                this.minimapLabelMaxZoom, this::setMinimapLabelMaxZoom, right, startY, columnWidth);
        this.addSlider("xaerorecruits.config.world_map_label_scale", 0.05D, 4.0D, 0.05D,
                this.worldMapLabelScale, this::setWorldMapLabelScale, left, startY + ROW_HEIGHT, columnWidth);
        this.addSlider("xaerorecruits.config.minimap_label_scale", 0.05D, 4.0D, 0.05D,
                this.minimapLabelScale, this::setMinimapLabelScale, right, startY + ROW_HEIGHT, columnWidth);
        this.addSlider("xaerorecruits.config.world_map_min_scale", 0.05D, 16.0D, 0.05D,
                this.worldMapLabelMinScale, this::setWorldMapLabelMinScale,
                left, startY + ROW_HEIGHT * 2, columnWidth);
        this.addSlider("xaerorecruits.config.world_map_max_scale", 0.05D, 50.0D, 0.05D,
                this.worldMapLabelMaxScale, this::setWorldMapLabelMaxScale,
                right, startY + ROW_HEIGHT * 2, columnWidth);
        this.addToggle("xaerorecruits.config.show_on_minimap", () -> this.showOnMinimap,
                this::setShowOnMinimap, left, startY + ROW_HEIGHT * 3, columnWidth);
        this.addToggle("xaerorecruits.config.show_minimap_info", () -> this.showMinimapInfo,
                this::setShowMinimapInfo, right, startY + ROW_HEIGHT * 3, columnWidth);
        this.addToggle("xaerorecruits.config.replace_recruits_map", () -> this.replaceRecruitsMap,
                this::setReplaceRecruitsMap, left, startY + ROW_HEIGHT * 4, columnWidth);


        int backWidth = Math.max(90, Math.min(120, columnWidth));
        int resetWidth = 60;
        int buttonGap = 6;
        int buttonsWidth = backWidth + buttonGap + resetWidth;
        int buttonsX = this.width / 2 - buttonsWidth / 2;
        int buttonsY = this.height - 32;

        this.addRenderableWidget(Button.builder(Component.translatable("xaerorecruits.config.back"), button -> this.onClose())
                .bounds(buttonsX, buttonsY, backWidth, 20)
                .build());
        Button reset = Button.builder(Component.translatable("xaerorecruits.config.reset"),
                        button -> this.resetToDefaults())
                .bounds(buttonsX + backWidth + buttonGap, buttonsY, resetWidth, 20)
                .build();
        reset.setTooltip(Tooltip.create(Component.translatable("xaerorecruits.config.reset.tooltip")));
        this.addRenderableWidget(reset);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dirty && System.currentTimeMillis() - this.lastChangeAt >= SAVE_DEBOUNCE_MS) {
            this.save();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void resetToDefaults() {
        this.showClaims = XRConfig.DEFAULT_SHOW_CLAIMS;
        this.showOnMinimap = XRConfig.DEFAULT_SHOW_ON_MINIMAP;
        this.showMinimapInfo = XRConfig.DEFAULT_SHOW_MINIMAP_INFO;
        this.replaceRecruitsMap = XRConfig.DEFAULT_REPLACE_RECRUITS_MAP;
        this.worldMapLabelScale = XRConfig.DEFAULT_WORLD_MAP_LABEL_SCALE;
        this.minimapLabelScale = XRConfig.DEFAULT_MINIMAP_LABEL_SCALE;
        this.minimapLabelMaxZoom = XRConfig.DEFAULT_MINIMAP_LABEL_MAX_ZOOM;
        this.worldMapLabelMinScale = XRConfig.DEFAULT_WORLD_MAP_LABEL_MIN_SCALE;
        this.worldMapLabelMaxScale = XRConfig.DEFAULT_WORLD_MAP_LABEL_MAX_SCALE;
        writeConfig(this.showClaims, this.showOnMinimap, this.showMinimapInfo, this.replaceRecruitsMap,
                this.worldMapLabelScale, this.minimapLabelScale, this.minimapLabelMaxZoom,
                this.worldMapLabelMinScale, this.worldMapLabelMaxScale);
        XaeroRefresher.refreshAll();
        this.markChanged();
        this.rebuildWidgets();
    }

    private void save() {
        this.dirty = false;
        try {
            XRConfig.SPEC.save();
        } catch (Throwable throwable) {
            XaeroRecruitsMod.LOGGER.warn("Could not save the client config", throwable);
        }
    }

    private void markChanged() {
        this.dirty = true;
        this.lastChangeAt = System.currentTimeMillis();
    }

    private static void writeConfig(boolean showClaims, boolean showOnMinimap, boolean showMinimapInfo,
                                    boolean replaceRecruitsMap, double worldMapLabelScale,
                                    double minimapLabelScale, double minimapLabelMaxZoom,
                                    double worldMapLabelMinScale, double worldMapLabelMaxScale) {
        XRConfig.Client config = XRConfig.CLIENT;
        config.showClaims.set(showClaims);
        config.showOnMinimap.set(showOnMinimap);
        config.showMinimapInfo.set(showMinimapInfo);
        config.replaceRecruitsMap.set(replaceRecruitsMap);
        config.worldMapLabelScale.set(worldMapLabelScale);
        config.minimapLabelScale.set(minimapLabelScale);
        config.minimapLabelMaxZoom.set(minimapLabelMaxZoom);
        config.worldMapLabelMinScale.set(worldMapLabelMinScale);
        config.worldMapLabelMaxScale.set(worldMapLabelMaxScale);
    }

    private void addToggle(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter,
                           int x, int y, int width) {
        Button button = Button.builder(toggleMessage(labelKey, getter.getAsBoolean()), pressed -> {
            boolean value = !getter.getAsBoolean();
            setter.accept(value);
            pressed.setMessage(toggleMessage(labelKey, value));
        }).bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable(labelKey + ".tooltip")));
        this.addRenderableWidget(button);
    }

    private void addSlider(String labelKey, double min, double max, double step,
                           double initial, DoubleConsumer setter, int x, int y, int width) {
        ConfigSlider slider = new ConfigSlider(labelKey, min, max, step, initial, setter, x, y, width);
        this.addRenderableWidget(slider);
    }

    private static Component toggleMessage(String labelKey, boolean value) {
        return Component.translatable(labelKey)
                .append(": ")
                .append(Component.translatable(value ? "xaerorecruits.config.on" : "xaerorecruits.config.off"));
    }

    private void setShowClaims(boolean value) {
        this.showClaims = value;
        XRConfig.CLIENT.showClaims.set(value);
        XaeroRefresher.refreshAll();
        this.markChanged();
    }

    private void setShowOnMinimap(boolean value) {
        this.showOnMinimap = value;
        XRConfig.CLIENT.showOnMinimap.set(value);
        XaeroRefresher.refreshAll();
        this.markChanged();
    }

    private void setShowMinimapInfo(boolean value) {
        this.showMinimapInfo = value;
        XRConfig.CLIENT.showMinimapInfo.set(value);
        XaeroRefresher.refreshAll();
        this.markChanged();
    }

    private void setReplaceRecruitsMap(boolean value) {
        this.replaceRecruitsMap = value;
        XRConfig.CLIENT.replaceRecruitsMap.set(value);
        this.markChanged();
    }

    private void setWorldMapLabelScale(double value) {
        this.worldMapLabelScale = value;
        XRConfig.CLIENT.worldMapLabelScale.set(value);
        this.markChanged();
    }

    private void setMinimapLabelScale(double value) {
        this.minimapLabelScale = value;
        XRConfig.CLIENT.minimapLabelScale.set(value);
        this.markChanged();
    }

    private void setMinimapLabelMaxZoom(double value) {
        this.minimapLabelMaxZoom = value;
        XRConfig.CLIENT.minimapLabelMaxZoom.set(value);
        this.markChanged();
    }

    private void setWorldMapLabelMinScale(double value) {
        this.worldMapLabelMinScale = value;
        XRConfig.CLIENT.worldMapLabelMinScale.set(value);
        this.markChanged();
    }

    private void setWorldMapLabelMaxScale(double value) {
        this.worldMapLabelMaxScale = value;
        XRConfig.CLIENT.worldMapLabelMaxScale.set(value);
        this.markChanged();
    }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String labelKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleConsumer setter;
        private double actual;

        private ConfigSlider(String labelKey, double min, double max, double step,
                             double initial, DoubleConsumer setter, int x, int y, int width) {
            super(x, y, width, 20, Component.empty(), normalize(initial, min, max));
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
            this.actual = Mth.clamp(initial, min, max);
            this.setTooltip(Tooltip.create(Component.translatable(labelKey + ".tooltip")));
            this.updateMessage();
        }

        @Override
        protected void applyValue() {
            double next = this.min + this.value * (this.max - this.min);
            if (this.step > 0.0D) {
                next = Math.round(next / this.step) * this.step;
            }
            this.actual = Mth.clamp(next, this.min, this.max);
            this.setter.accept(this.actual);
        }

        @Override
        protected void updateMessage() {
            String value = String.format(Locale.ROOT, "%.2f", this.actual);
            this.setMessage(Component.translatable(this.labelKey).append(": ").append(value));
        }

        private static double normalize(double value, double min, double max) {
            if (max <= min) {
                return 0.0D;
            }
            return Mth.clamp((value - min) / (max - min), 0.0D, 1.0D);
        }
    }
}
