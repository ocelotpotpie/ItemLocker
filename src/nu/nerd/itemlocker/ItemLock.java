package nu.nerd.itemlocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

// ----------------------------------------------------------------------------
/**
 * Accesses lock information in the scoreboard tags of an ItemFrame or
 * ArmorStand.
 * 
 * To be "locked" an entity must have a non-null owner (stored as a player UUID
 * string). Locked entities can optionally have a region name listing players
 * who can run commands affecting the entity.
 */
public class ItemLock {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * Permission attributes are initialised from the scoreboard tags of the
     * specified frame or stand.
     * 
     * @param entity the item frame or armour stand entity.
     */
    public ItemLock(Entity entity) {
        _entity = entity;
        parseTags();
    }

    // ------------------------------------------------------------------------
    /**
     * Unlock the frame/stand.
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
     * the frame/stand.
     * 
     * This could be because it is unlocked, the player owns the frame/stand, or
     * is in the frame/stand's region as a region owner or member.
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
            _entity.addScoreboardTag(OWNER + ownerUuid.toString());
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
            Location loc = _entity.getLocation();
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
            _entity.addScoreboardTag(REGION + regionName);
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
        _entity.addScoreboardTag(ACCESS + group.getCode());
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
        _entity.addScoreboardTag(ROTATE + group.getCode());
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
     * Return the type of the locked entity, for presentation to the user.
     * 
     * @return the type of the locked entity, for presentation to the user.
     */
    public String getEntityType() {
        return isFrame() ? "item frame" : "armor stand";
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this lock is for an ArmorStand; false for an ItemFrame.
     * 
     * @return true if this lock is for an ArmorStand; false for an ItemFrame.
     */
    public boolean isStand() {
        return !isFrame();
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this lock is for an ItemFrame; false for an ArmorStand.
     * 
     * @return true if this lock is for an ItemFrame; false for an ArmorStand.
     */
    public boolean isFrame() {
        return (_entity instanceof ItemFrame);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the entity holds no items.
     * 
     * @return true if the entity holds no items.
     */
    public boolean isEmpty() {
        // NOTE: Entity.isEmpty() tests for vehicle passenger.
        if (isFrame()) {
            ItemFrame frame = (ItemFrame) _entity;
            return isNothing(frame.getItem());
        } else {
            ArmorStand stand = (ArmorStand) _entity;
            for (ItemStack item : stand.getEquipment().getArmorContents()) {
                if (!isNothing(item)) {
                    return false;
                }
            }
            if (!isNothing(stand.getEquipment().getItemInMainHand()) ||
                !isNothing(stand.getEquipment().getItemInOffHand())) {
                return false;
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a list of items held by the frame or stand.
     * 
     * @return a list of items held by the frame or stand.
     */
    public List<ItemStack> getItems() {
        if (isFrame()) {
            ItemFrame frame = (ItemFrame) _entity;
            if (!isNothing(frame.getItem())) {
                return Arrays.asList(frame.getItem());
            } else {
                return new LinkedList<ItemStack>();
            }
        } else {
            ArmorStand stand = (ArmorStand) _entity;
            ArrayList<ItemStack> standItems = new ArrayList<>();
            for (ItemStack item : stand.getEquipment().getArmorContents()) {
                if (!isNothing(item)) {
                    standItems.add(item);
                }
            }
            ItemStack mainHand = stand.getEquipment().getItemInMainHand();
            if (!isNothing(mainHand)) {
                standItems.add(mainHand);
            }
            ItemStack offHand = stand.getEquipment().getItemInOffHand();
            if (!isNothing(offHand)) {
                standItems.add(offHand);
            }
            return standItems;
        }
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
        return _entity.getType() + " owner: " + ownerName + ", region: " + getRegionName() +
               ", access: " + getAccessGroup() + ", rotate: " + getRotateGroup();
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if an item held in a stand or frame is nothing.
     * 
     * @param item the ItemStack.
     * @return true if an item held in a stand or frame is nothing.
     */
    protected boolean isNothing(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    // ------------------------------------------------------------------------
    /**
     * Parse the scoreboard tags of the frame to extract lock permissions.
     */
    protected void parseTags() {
        for (String tag : _entity.getScoreboardTags()) {
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
        for (String tag : _entity.getScoreboardTags()) {
            if (tag.startsWith(prefix)) {
                _entity.removeScoreboardTag(tag);
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
     * The item frame or armour stand whose permissions are accessed.
     */
    protected Entity _entity;

    /**
     * The UUID of the lock owner.
     */
    protected UUID _ownerUuid;

    /**
     * The region name of the lock.
     */
    protected String _regionName;

    /**
     * The group who can access (put into and take from) the frame/stand.
     */
    protected PermissionGroup _accessGroup = PermissionGroup.MEMBERS;

    /**
     * The group who can rotate the frame/stand.
     */
    protected PermissionGroup _rotateGroup = PermissionGroup.NOBODY;

} // class ItemLock