package com.aguilam.xaerorecruits.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class XRConfig {
    public static final int CLAIM_FILL_OPACITY = 40;
    public static final int CLAIM_BORDER_OPACITY = 55;

    public static final boolean DEFAULT_SHOW_CLAIMS = true;
    public static final boolean DEFAULT_SHOW_ON_MINIMAP = true;
    public static final boolean DEFAULT_SHOW_MINIMAP_INFO = true;
    public static final boolean DEFAULT_REPLACE_RECRUITS_MAP = true;
    public static final double DEFAULT_WORLD_MAP_LABEL_SCALE = 1.0D;
    public static final double DEFAULT_MINIMAP_LABEL_SCALE = 0.35D;
    public static final double DEFAULT_MINIMAP_LABEL_MAX_ZOOM = 4.0D;
    public static final double DEFAULT_WORLD_MAP_LABEL_MIN_SCALE = 0.25D;
    public static final double DEFAULT_WORLD_MAP_LABEL_MAX_SCALE = 4.0D;

    public static final ForgeConfigSpec SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        SPEC = builder.build();
    }

    private XRConfig() {
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue showClaims;
        public final ForgeConfigSpec.BooleanValue showOnMinimap;
        public final ForgeConfigSpec.BooleanValue showMinimapInfo;
        public final ForgeConfigSpec.BooleanValue replaceRecruitsMap;

        public final ForgeConfigSpec.DoubleValue worldMapLabelScale;
        public final ForgeConfigSpec.DoubleValue minimapLabelScale;
        public final ForgeConfigSpec.DoubleValue minimapLabelMaxZoom;
        public final ForgeConfigSpec.DoubleValue worldMapLabelMinScale;
        public final ForgeConfigSpec.DoubleValue worldMapLabelMaxScale;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Claim highlighting settings").push("claims");
            this.showClaims = builder
                    .comment("Show Recruits faction claims on Xaero's minimap and world map.")
                    .define("showClaims", DEFAULT_SHOW_CLAIMS);
            this.showOnMinimap = builder
                    .comment("Show Recruits faction names drawn on top of the minimap.",
                            "Turn this off to hide only those labels; the claim info under the minimap is not affected.")
                    .define("showOnMinimap", DEFAULT_SHOW_ON_MINIMAP);
            this.showMinimapInfo = builder
                    .comment("Show the Recruits claim info under the minimap (claim name and faction).",
                            "Turn this off to hide only that text; the labels on the minimap are not affected.")
                    .define("showMinimapInfo", DEFAULT_SHOW_MINIMAP_INFO);
            builder.pop();

            builder.comment("Recruits integration settings").push("integration");
            this.replaceRecruitsMap = builder
                    .comment("Open Xaero's world map instead of the Recruits map from the Claim button in the faction screen.")
                    .define("replaceRecruitsMap", DEFAULT_REPLACE_RECRUITS_MAP);
            builder.pop();

            builder.comment("Faction label settings").push("labels");
            this.worldMapLabelMinScale = builder
            .comment("Hide world-map labels when the map scale (pixels per block) is lower than this value.",
                    "Default 0.25: labels disappear when zoomed out below 0.25x.")
            .defineInRange("worldMapLabelMinScale", DEFAULT_WORLD_MAP_LABEL_MIN_SCALE, 0.05D, 16.0D);
            this.worldMapLabelMaxScale = builder
            .comment("Hide world-map labels when the map scale is higher than this value.",
                    "Default 4.00: labels disappear when zoomed in past 4x.")
            .defineInRange("worldMapLabelMaxScale", DEFAULT_WORLD_MAP_LABEL_MAX_SCALE, 0.05D, 50.0D);
            this.worldMapLabelScale = builder
                    .comment("Multiplier for the faction label size on the world map.",
                            "The base size is calibrated to Xaero's world-map waypoint labels.")
                    .defineInRange("worldMapLabelScale", DEFAULT_WORLD_MAP_LABEL_SCALE, 0.05D, 4.0D);
            this.minimapLabelScale = builder
                    .comment("Multiplier for the faction label size on the minimap.",
                            "0.35 keeps the labels readable without covering the map.")
                    .defineInRange("minimapLabelScale", DEFAULT_MINIMAP_LABEL_SCALE, 0.05D, 4.0D);
            this.minimapLabelMaxZoom = builder
                    .comment("Hide minimap labels when the minimap zoom is higher than this value.",
                            "Higher minimap zoom means a closer view. The default hides labels only at very close zoom.")
                    .defineInRange("minimapLabelMaxZoom", DEFAULT_MINIMAP_LABEL_MAX_ZOOM, 0.0D, 10.0D);
            builder.pop();
        }
    }
}
