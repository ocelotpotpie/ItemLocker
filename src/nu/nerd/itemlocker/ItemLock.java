package nu.nerd.itemlocker;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

// ----------------------------------------------------------------------------
/**
 * Accesses lock information in the scoreboard tags of an ItemFrame.
 * 
 * To be "locked" an ItemFrame must have a non-null owner (stored as a player
 * UUID string). Locked ItemFrames can optionally have a region name listing
 * players who can run commands affecting the ItemFrame.
 * 
 * Some common scenarios:
 * <ul>
 * <li>A town labels their chests with items in item frames. The items are
 * locked to the town region so any town member can administer the frames. The
 * rotate flag is set to false so the items will always stay the right way up.
 * The access flag can be set true or false depending on whether the chests are
 * being relabelled or not. Nobody outside the town can alter the frames.
 * Because the frames are on LWC locked chests, they cannot be taken down by
 * anyone until the frames are unlocked.</li>
 * <li>A town makes a rail station selector dial with an item frame. The frame
 * is put in a WorldGuard build: allow region so that anyone can interact with
 * it (this is the intended behaviour for WorldGuard). The access option is set
 * false, so the item cannot be removed. The rotate and public options are set
 * true, so any player can rotate the dial.</li>
 * </ul>
 */
public class ItemLock {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * Permission attributes are initialised from the scoreboard tags of the
     * specified frame.
     */
    public ItemLock(ItemFrame frame) {
        _frame = frame;
        parseTags();
    }

    // ------------------------------------------------------------------------
    /**
     * Unlock the frame.
     */
    public void unlock() {
        setOwnerUuid(null);
        setRegionName(null);
        setAccessGroup(PermissionGroup.MEMBERS);
        setRotateGroup(PermissionGroup.NOBODY);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified player is permitted to execute commands on
     * the item frame.
     * 
     * This could be because it is unlocked, the player owns the item frame, or
     * is in the item frame's region as a region owner or member.
     * 
     * @param player the player.
     * @return true if the frame is unlocked or the player is in the frame's
     *         permitted player set.
     */
    public boolean permits(Player player) {
        return !isOwned() || isOwnerOrMember(player);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified player is the owner of, or an owner/member
     * of the region of the frame.
     * 
     * @param player the player.
     * @return true if the specified player is the owner of, or an owner/member
     *         of the region of the frame.
     */
    public boolean isOwnerOrMember(Player player) {
        if (isOwnedBy(player)) {
            return true;
        }

        ProtectedRegion region = getRegion();
        if (region != null) {
            LocalPlayer local = ItemLocker.PLUGIN.getWorldGuard().wrapPlayer(player);
            do {
                if (region.isOwner(local) || region.isMember(local)) {
                    return true;
                }
                region = region.getParent();
            } while (region != null);
        }

        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the player can put or take the item in the frame.
     * 
     * @param player the player.
     * @return true if the player can put or take the item in the frame.
     */
    public boolean canBeAccessedBy(Player player) {
        return !isOwned() ||
               getAccessGroup() == PermissionGroup.PUBLIC ||
               (getAccessGroup() == PermissionGroup.MEMBERS && isOwnerOrMember(player));
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the player can rotate the frame.
     * 
     * @param player the player.
     * @return true if the player can rotate the frame.
     */
    public boolean canBeRotatedBy(Player player) {
        return !isOwned() ||
               getRotateGroup() == PermissionGroup.PUBLIC ||
               (getRotateGroup() == PermissionGroup.MEMBERS && isOwnerOrMember(player));
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this frame has an owner.
     * 
     * @return true if this frame has an owner.
     */
    public boolean isOwned() {
        return getOwnerUuid() != null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this frame is owned by the specified player.
     * 
     * @return true if this frame is owned by the specified player.
     */
    public boolean isOwnedBy(Player player) {
        return player != null && player.getUniqueId().equals(getOwnerUuid());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the owner of the lock, or null if not locked.
     * 
     * @return the owner of the lock, or null if not locked.
     */
    public OfflinePlayer getOwner() {
        return getOwnerUuid() != null ? Bukkit.getOfflinePlayer(getOwnerUuid()) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the UUID of the owner of the lock, or null if not locked.
     * 
     * @return the UUID of the owner of the lock, or null if not locked.
     */
    public UUID getOwnerUuid() {
        return _ownerUuid;
    }

    // ------------------------------------------------------------------------

    public void setOwnerUuid(UUID ownerUuid) {
        _ownerUuid = ownerUuid;
        removeTagWithPrefix(OWNER);
        if (ownerUuid != null) {
            _frame.addScoreboardTag(OWNER + ownerUuid.toString());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the name of the region of the lock.
     * 
     * @return the name of the region of the lock.
     */
    public String getRegionName() {
        return _regionName;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the WorldGuard region of the lock.
     * 
     * @return the WorldGuard region of the lock.
     */
    public ProtectedRegion getRegion() {
        if (getRegionName() == null) {
            return null;
        } else {
            Location loc = _frame.getLocation();
            WorldGuardPlugin wg = ItemLocker.PLUGIN.getWorldGuard();
            RegionManager manager = wg.getRegionManager(loc.getWorld());
            return manager.getRegion(getRegionName());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Set the name of the region of the lock.
     * 
     * @param regionName the region name.
     */
    public void setRegionName(String regionName) {
        _regionName = regionName;
        removeTagWithPrefix(REGION);
        if (regionName != null && !regionName.isEmpty()) {
            _frame.addScoreboardTag(REGION + regionName);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Set the group that can access the frame.
     * 
     * @param group the group.
     */
    public void setAccessGroup(PermissionGroup group) {
        _accessGroup = group;
        removeTagWithPrefix(ACCESS);
        _frame.addScoreboardTag(ACCESS + group.getCode());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the group that can access the frame.
     * 
     * @return the group that can access the frame.
     */

    public PermissionGroup getAccessGroup() {
        return _accessGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the group that can rotate the frame.
     * 
     * @param group the group.
     */
    public void setRotateGroup(PermissionGroup group) {
        _rotateGroup = group;
        removeTagWithPrefix(ROTATE);
        _frame.addScoreboardTag(ROTATE + group.getCode());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the group that can access the frame.
     * 
     * @return the group that can access the frame.
     */
    public PermissionGroup getRotateGroup() {
        return _rotateGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a string representation useful in debugging.
     * 
     * @return a string representation useful in debugging.
     */
    @Override
    public String toString() {
        OfflinePlayer owner = getOwner();
        String ownerName = (owner != null ? owner.getName() : "<nobody>");
        return "owner: " + ownerName + ", region: " + getRegionName() +
               ", access: " + getAccessGroup() + ", rotate: " + getRotateGroup();
    }

    // ------------------------------------------------------------------------
    /**
     * Parse the scoreboard tags of the frame to extract lock permissions.
     */
    protected void parseTags() {
        for (String tag : _frame.getScoreboardTags()) {
            if (tag.startsWith(OWNER)) {
                try {
                    _ownerUuid = UUID.fromString(tag.substring(OWNER.length()));
                } catch (IllegalArgumentException ex) {
                }
            } else if (tag.startsWith(REGION)) {
                _regionName = tag.substring(REGION.length());
            } else if (tag.startsWith(ACCESS)) {
                _accessGroup = PermissionGroup.fromCode(tag.charAt(ACCESS.length()));
            } else if (tag.startsWith(ROTATE)) {
                _rotateGroup = PermissionGroup.fromCode(tag.charAt(ROTATE.length()));
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the tag whose id begins with the specified prefix.
     * 
     * @param prefix the prefix of the tag to match.
     */
    protected void removeTagWithPrefix(String prefix) {
        for (String tag : _frame.getScoreboardTags()) {
            if (tag.startsWith(prefix)) {
                _frame.removeScoreboardTag(tag);
                break;
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prefix of tags that store the lock owner's UUID
     */
    protected static final String OWNER = "owner:";

    /**
     * Prefix of tags that store the lock's region name.
     */
    protected static final String REGION = "region:";

    /**
     * Prefix of tags that store the lock's access group.
     */
    protected static final String ACCESS = "access:";

    /**
     * Prefix of tags that store the lock's rotate group.
     */
    protected static final String ROTATE = "rotate:";

    /**
     * The item frame whose permissions are accessed.
     */
    protected ItemFrame _frame;

    /**
     * The UUID of the lock owner.
     */
    protected UUID _ownerUuid;

    /**
     * The region name of the lock.
     */
    protected String _regionName;

    /**
     * The group who can access (put into and take from) the item frame.
     */
    protected PermissionGroup _accessGroup = PermissionGroup.MEMBERS;

    /**
     * The group who can rotate the item frame.
     */
    protected PermissionGroup _rotateGroup = PermissionGroup.NOBODY;

} // class ItemLock