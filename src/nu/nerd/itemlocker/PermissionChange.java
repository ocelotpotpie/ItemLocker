package nu.nerd.itemlocker;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;

// ----------------------------------------------------------------------------
/**
 * Represents a change to the permissions of an item frame.
 * 
 * Each of the getters returns null if the permission setting was not specified
 * in the arguments to {@link PermissionChange#parse()}.
 */
public class PermissionChange {
    // ------------------------------------------------------------------------
    /**
     * Parse a change of permissions from the arguments of /ilock or /imodify.
     * 
     * @param player the sending player.
     * @param args the command arguments.
     * @return either a PermissionChange instance encoding the arguments, or a
     *         String containing the error to show to the command sender.
     */
    public static Object parse(Player player, String[] args) {
        OfflinePlayer owner = null;
        String regionName = null;
        PermissionGroup accessGroup = null;
        PermissionGroup rotateGroup = null;

        for (String arg : args) {
            // Is it r:<region>?
            if (arg.startsWith("r:")) {
                if (regionName != null) {
                    return "You can't set more than one region name!";
                } else {
                    regionName = arg.substring(2);
                    // If not '-' (remove region), check for valid region.
                    if (!regionName.equals("-")) {
                        WorldGuardPlugin wg = ItemLocker.PLUGIN.getWorldGuard();
                        RegionManager manager = wg.getRegionManager(player.getLocation().getWorld());
                        if (manager.getRegion(regionName) == null) {
                            return "There's no region named '" + regionName + "' in this world!";
                        }
                    }
                }
            } else {
                // Is it -/+/* access/rotate?
                PermissionGroup group = PermissionGroup.fromCode(arg.charAt(0));
                if (group != null) {
                    String permission = arg.substring(1).toLowerCase();
                    if (permission.equals("access") || permission.equals("a")) {
                        if (accessGroup != null) {
                            return "You can't set more than one access group!";
                        } else {
                            accessGroup = group;
                        }
                    } else if (permission.equals("rotate") || permission.equals("r")) {
                        if (rotateGroup != null) {
                            return "You can't set more than one rotate group!";
                        } else {
                            rotateGroup = group;
                        }
                    } else {
                        return "You can only grant permission to \"access\" or \"rotate\".";
                    }
                } else {
                    // Must be <owner>.
                    if (owner != null) {
                        return "You can't specify more than one owner!";
                    } else {
                        if (!player.hasPermission("itemlocker.bypass")) {
                            return "Only admins can lock frames to a specified player!";
                        }

                        owner = Bukkit.getOfflinePlayer(arg);
                        if (!owner.hasPlayedBefore()) {
                            return arg + " has never played on this server!";
                        }
                    }
                }
            }
        } // for

        return new PermissionChange(owner, regionName, accessGroup, rotateGroup);
    } // parse

    // ------------------------------------------------------------------------
    /**
     * Return a new PermissionChange instance with region and groups overridden
     * by the specified instance.
     * 
     * If any of the region name, access group, or rotate group of the change
     * are non null, they will take precedence over the attributes copied from
     * this instance.
     * 
     * @param change the instance whose non-null attributes take precendence in
     *        the result.
     * @return a new PermissionChange instance based on this, but with
     *         attributes overridden by the non-null attributes of the
     *         parameter.
     */
    public PermissionChange overriddenBy(PermissionChange change) {
        return new PermissionChange(getOwner(),
            (change.getRegionName() != null) ? change.getRegionName() : getRegionName(),
            (change.getAccessGroup() != null) ? change.getAccessGroup() : getAccessGroup(),
            (change.getRotateGroup() != null) ? change.getRotateGroup() : getRotateGroup());
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param owner the new owner of the frame.
     * @param regionName the name of the new region of the frame.
     * @param accessGroup the new group that can access the frame.
     * @param rotateGroup the new group that can rotate the frame.
     */
    public PermissionChange(OfflinePlayer owner, String regionName,
                            PermissionGroup accessGroup, PermissionGroup rotateGroup) {
        _owner = owner;
        _regionName = regionName;
        _accessGroup = accessGroup;
        _rotateGroup = rotateGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the new owner of the frame.
     * 
     * @return the new owner of the frame.
     */
    public OfflinePlayer getOwner() {
        return _owner;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the name of the new region of the frame.
     * 
     * @return the name of the new region of the frame.
     */
    public String getRegionName() {
        return _regionName;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the new group that can access the frame.
     * 
     * @return the new group that can access the frame.
     */
    public PermissionGroup getAccessGroup() {
        return _accessGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the new group that can rotate the frame.
     * 
     * @return the new group that can rotate the frame.
     */
    public PermissionGroup getRotateGroup() {
        return _rotateGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * The new owner of the frame.
     */
    protected OfflinePlayer _owner;

    /**
     * The name of the new region of the frame.
     */
    protected String _regionName;

    /**
     * The new group that can access the frame.
     */
    protected PermissionGroup _accessGroup;

    /**
     * The new group that can rotate the frame.
     */
    protected PermissionGroup _rotateGroup;

} // class PermissionChange