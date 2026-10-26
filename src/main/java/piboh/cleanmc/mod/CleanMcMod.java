package piboh.cleanmc.mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared identifiers for CleanMCMod.
 *
 * <p>Everything in this class is loader-agnostic. All Minecraft-facing code lives in
 * {@code piboh.cleanmc.mod.client}, {@code ...command} and {@code ...ui}; the cache engine in
 * {@code ...clean} must stay free of Minecraft classes so it can be unit tested headlessly.</p>
 */
public final class CleanMcMod {
    public static final String MOD_ID = "cleanmcmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private CleanMcMod() {
    }
}
