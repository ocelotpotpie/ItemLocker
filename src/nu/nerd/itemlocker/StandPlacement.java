package nu.nerd.itemlocker;

import org.bukkit.Location;
import org.bukkit.entity.Player;

// ----------------------------------------------------------------------------
/**
 * Records the when, where and who of a player placing an armour stand.
 * 
 * The Bukkit API doesn't provide an event that says which player spawned an
 * armour stand, so we use instances of this class to correlate that information
 * between the PlayerInteractEvent that triggers the spawn and the
 * CreatureSpawnEvent when the stand spawns.
 */
public class StandPlacement {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param player the player that placed the stand.
     * @param loc the location where the stand was placed.
     */
    public StandPlacement(Player player, Location loc) {
        _player = player;
        _location = loc;
        _time = loc.getWorld().getFullTime();
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this placement matches the specified location.
     * 
     * The full world time is derived from the location's world.
     * 
     * @param loc the location of the CreatureSpawnEvent of an ArmorStand.
     * @return true if this StandPlacement is for the same stand.
     */
    public boolean matches(Location loc) {
        return _time == loc.getWorld().getFullTime() &&
               _location.getWorld() == loc.getWorld() &&
               _location.getBlockX() == loc.getBlockX() &&
               _location.getBlockY() == loc.getBlockY() &&
               _location.getBlockZ() == loc.getBlockZ();
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this placement is out of date and should be discarded.
     * 
     * @param loc the location where an ArmorStand spawned, used to compute the
     *        world full time at that location.
     * @return true if this placement is too old.
     */
    public boolean isOutdated(Location loc) {
        return _time < loc.getWorld().getFullTime();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the player that placed the armour stand.
     * 
     * @return the player that placed the armour stand.
     */
    public Player getPlayer() {
        return _player;
    }

    // ------------------------------------------------------------------------
    /**
     * The player that placed the stand.
     */
    private final Player _player;

    /**
     * The Location where the stand was placed.
     */
    private final Location _location;

    /**
     * The World's full time when the stand was placed.
     */
    private final long _time;

} // class StandPlacement