package com.walksy.shieldstatus.client;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue INTERPOLATE;
    public static final ForgeConfigSpec.IntValue ENABLED_RED, ENABLED_GREEN, ENABLED_BLUE;
    public static final ForgeConfigSpec.IntValue DISABLED_RED, DISABLED_GREEN, DISABLED_BLUE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("shield_status");
        ENABLED = b.define("enabled", true);
        INTERPOLATE = b.define("interpolate", false);
        ENABLED_RED = b.defineInRange("enabled_red", 0, 0, 255);
        ENABLED_GREEN = b.defineInRange("enabled_green", 255, 0, 255);
        ENABLED_BLUE = b.defineInRange("enabled_blue", 0, 0, 255);
        DISABLED_RED = b.defineInRange("disabled_red", 255, 0, 255);
        DISABLED_GREEN = b.defineInRange("disabled_green", 0, 0, 255);
        DISABLED_BLUE = b.defineInRange("disabled_blue", 0, 0, 255);
        b.pop();
        SPEC = b.build();
    }
    private ClientConfig() {}
}
