package nu.nerd.itemlocker;

import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

//-----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    // ------------------------------------------------------------------------
    /**
     * If true, log the configuration on reload.
     */
    public boolean DEBUG_CONFIG;

    /**
     * If true, log events for debugging.
     */
    public boolean DEBUG_EVENTS;

    /**
     * If true, locked item frames that have their backing removed simply float
     * in the air rather than break.
     */
    public boolean FLOATING_FRAMES;

    /**
     * If true, item frames automatically lock to their containing region when
     * placed.
     */
    public boolean AUTO_LOCK;

    /**
     * If true, when auto locking an item frame, add the most specific enclosing
     * region.
     */
    public boolean AUTO_LOCK_REGION;

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        ItemLocker.PLUGIN.reloadConfig();
        FileConfiguration config = ItemLocker.PLUGIN.getConfig();
        Logger logger = ItemLocker.PLUGIN.getLogger();

        DEBUG_CONFIG = config.getBoolean("debug.config");
        DEBUG_EVENTS = config.getBoolean("debug.events");
        FLOATING_FRAMES = config.getBoolean("floating-frames");
        AUTO_LOCK = config.getBoolean("auto-lock");
        AUTO_LOCK_REGION = config.getBoolean("auto-lock-region");

        if (DEBUG_CONFIG) {
            logger.info("Configuration:");
            logger.info("DEBUG_EVENTS: " + DEBUG_EVENTS);
            logger.info("FLOATING_FRAMES: " + FLOATING_FRAMES);
            logger.info("AUTO_LOCK: " + AUTO_LOCK);
            logger.info("AUTO_LOCK_REGION: " + AUTO_LOCK_REGION);
        }
    } // reload
} // class Configuration