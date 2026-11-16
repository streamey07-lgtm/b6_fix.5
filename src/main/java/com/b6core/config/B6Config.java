package com.b6core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * All player-facing settings. COMMON affects gameplay, CLIENT affects only
 * visuals and sound on the player's own screen.
 */
public final class B6Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // --- COMMON (gameplay) ---
    public static final ForgeConfigSpec.DoubleValue ENCOUNTER_FREQUENCY;
    public static final ForgeConfigSpec.EnumValue<Difficulty> B6_DIFFICULTY;
    public static final ForgeConfigSpec.IntValue STORY_STAGE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_MODE;
    public static final ForgeConfigSpec.BooleanValue LOOK_TELEPORT;

    // --- CLIENT (visuals / audio) ---
    public static final ForgeConfigSpec.DoubleValue OVERLAY_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.BooleanValue MENU_QUIT_PRANK;
    public static final ForgeConfigSpec.BooleanValue SAVE_QUIT_HORROR;
    public static final ForgeConfigSpec.BooleanValue MAIN_MENU_HORROR;

    public enum Difficulty { EASY, NORMAL, HARD, NIGHTMARE }

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("gameplay");
        ENCOUNTER_FREQUENCY = common
                .comment("How often B6 encounters happen. 0 = never, 1 = default, 3 = very frequent.")
                .defineInRange("encounterFrequency", 1.0, 0.0, 3.0);
        B6_DIFFICULTY = common
                .comment("How dangerous B6 is.")
                .defineEnum("b6Difficulty", Difficulty.NORMAL);
        STORY_STAGE = common
                .comment("Story progress (1-17, matches the story list). Change only to skip or replay parts.")
                .defineInRange("storyStage", 1, 1, 17);
        DEBUG_MODE = common
                .comment("Shows debug info in the log and chat.")
                .define("debugMode", false);
        LOOK_TELEPORT = common
                .comment("If true, staring at a Missing Texture block for about 2 seconds teleports you to the Core dimension. "
                        + "Use /b6core leave inside the Core to return.")
                .define("lookTeleport", false);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("visuals_and_audio");
        OVERLAY_INTENSITY = client
                .comment("Strength of glitch, static and corruption screen effects. 0 = off, 1 = default, 2 = extreme.")
                .defineInRange("overlayIntensity", 1.0, 0.0, 2.0);
        SOUND_VOLUME = client
                .comment("Volume multiplier for B6 sounds. 0 = silent, 1 = default.")
                .defineInRange("soundVolume", 1.0, 0.0, 1.0);
        MENU_QUIT_PRANK = client
                .comment("Main menu horror event: the Quit Game button corrupts and melts after 3 clicks. "
                        + "Visual only - Alt+F4 and the window close button always work. Set false to disable.")
                .define("menuQuitPrank", true);
        SAVE_QUIT_HORROR = client
                .comment("From day 6, Save and Quit to Title corrupts, melts and then stops working. Clicks do nothing "
                        + "and it tells the player to use Alt+F4. Alt+F4 and the window close button always work. "
                        + "Set false to disable.")
                .define("saveQuitHorror", true);
        MAIN_MENU_HORROR = client
                .comment("If the title screen is left alone for 1 minute, B6 appears behind the buttons, splits Options, "
                        + "destroys the other buttons and hands you Singleplayer back, corroded but still clickable. "
                        + "Set false to disable.")
                .define("mainMenuHorror", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private B6Config() {}
}
