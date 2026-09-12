package dev.reny.optimization.compat;

/** Optimization/runtime mods for which Reny has explicit compatibility policy. */
public enum KnownMod {

    FORGE_FML("forge-fml", "Forge/FML", "forge", "fml"),
    UNIMIXINS("unimixins", "UniMixins", "unimixins"),
    ANGELICA("angelica", "Angelica", "angelica"),
    ARCHAIC_FIX("archaicfix", "ArchaicFix", "archaicfix"),
    FALSE_TWEAKS("falsetweaks", "FalseTweaks", "falsetweaks", "false_tweaks"),
    OPTIFINE("optifine", "OptiFine", "optifine"),
    LWJGL3IFY("lwjgl3ify", "LWJGL3ify", "lwjgl3ify", "lwjgl3ify-next");

    private final String key;
    private final String displayName;
    private final String[] aliases;

    KnownMod(String key, String displayName, String... aliases) {
        this.key = key;
        this.displayName = displayName;
        this.aliases = aliases.clone();
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getAliases() {
        return aliases.clone();
    }
}
