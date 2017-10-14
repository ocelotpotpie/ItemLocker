package nu.nerd.itemlocker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 */
public class ItemLocker extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * This plugin as a singleton.
     */
    public static ItemLocker PLUGIN;

    /**
     * Configuration as a singleton.
     */
    public static final Configuration CONFIG = new Configuration();

    // ------------------------------------------------------------------------
    /**
     * Return the WorldGuard plugin.
     * 
     * @return the WorldGuard plugin.
     */
    public WorldGuardPlugin getWorldGuard() {
        return _worldGuard;
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        CONFIG.reload();

        Bukkit.getPluginManager().registerEvents(this, this);

        _worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        if (commandName.equals("itemlocker")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                CONFIG.reload();
                sender.sendMessage(ChatColor.GOLD + getName() + " configuration reloaded.");
            } else {
                invalidArgumentsMessage(sender, commandName);
            }
            return true;

        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be in-game to use this command!");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 1 && args[0].equals("help")) {
            return false;
        }

        if (commandName.equals("ibypass")) {
            if (args.length > 0) {
                invalidArgumentsMessage(sender, commandName);
                return true;
            }

            boolean isBypassing = (getMetadata(player, BYPASS_KEY) != null);
            if (isBypassing) {
                player.sendMessage(ChatColor.GOLD + "You will no longer bypass item frame permission checks.");
                player.removeMetadata(BYPASS_KEY, this);
            } else {
                player.sendMessage(ChatColor.GOLD + "You can now bypass item frame permission checks!");
                player.setMetadata(BYPASS_KEY, new FixedMetadataValue(this, null));
            }
            return true;

        } else if (commandName.equals("ipersist")) {
            if (args.length > 0) {
                invalidArgumentsMessage(sender, commandName);
                return true;
            }

            boolean isPersistent = (getMetadata(player, PERSIST_KEY) != null);
            if (isPersistent) {
                player.sendMessage(ChatColor.GOLD + "Your item frame actions will no longer persist.");
                player.removeMetadata(PERSIST_KEY, this);

                // Clear the previous action.
                player.removeMetadata(ACTION_KEY, this);
            } else {
                player.sendMessage(ChatColor.GOLD + "Your item frame actions will now persist.");
                player.setMetadata(PERSIST_KEY, new FixedMetadataValue(this, null));
            }
            return true;

        } else {
            // /ilock, /iunlock and /iinfo all set the ACTION_KEY metadata.
            // commandArgs stores information about the command in the metadata.
            HashMap<String, Object> commandArgs = new HashMap<>();
            if (commandName.equals("ilock")) {
                Object parsed = PermissionChange.parse(player, args);
                if (parsed instanceof String) {
                    sender.sendMessage(ChatColor.RED + (String) parsed);
                    return true;
                } else {
                    PermissionChange permissions = (PermissionChange) parsed;
                    OfflinePlayer owner = (permissions.getOwner() != null ? permissions.getOwner() : player);
                    String ownerMessage = player.equals(owner) ? "you" : owner.getName();
                    sender.sendMessage(ChatColor.GREEN + "The next item frame you right click will be locked to " +
                                       ChatColor.YELLOW + ownerMessage + ChatColor.GREEN + ".");
                    commandArgs.put("permissions", permissions);
                }

            } else if (commandName.equals("imodify")) {
                if (args.length == 0) {
                    invalidArgumentsMessage(sender, commandName);
                    return true;
                }

                Object parsed = PermissionChange.parse(player, args);
                if (parsed instanceof String) {
                    sender.sendMessage(ChatColor.RED + (String) parsed);
                    return true;
                } else {
                    PermissionChange permissions = (PermissionChange) parsed;
                    sender.sendMessage(ChatColor.GREEN + "Click on an item frame to change its permissions.");
                    commandArgs.put("permissions", permissions);
                }

            } else if (commandName.equals("iunlock")) {
                if (args.length > 0) {
                    invalidArgumentsMessage(sender, commandName);
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Right click an item frame to unlock it.");

            } else if (commandName.equals("iinfo")) {
                if (args.length > 0) {
                    invalidArgumentsMessage(sender, commandName);
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Right click an item frame to see who owns it.");
            }

            // Drop the leading 'i' in the command name.
            commandArgs.put("command", commandName.substring(1));
            FixedMetadataValue meta = new FixedMetadataValue(this, commandArgs);
            player.setMetadata(ACTION_KEY, meta);

            return true;
        }
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking item frames by extracting information about
     * their last command and /ipersist state from player metadata.
     */
    @SuppressWarnings("unchecked")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() != EntityType.ITEM_FRAME) {
            return;
        }

        ItemFrame frame = (ItemFrame) entity;
        ItemLock lock = new ItemLock(frame);
        Player player = event.getPlayer();
        boolean bypassing = (getMetadata(player, BYPASS_KEY) != null);

        if (CONFIG.DEBUG_EVENTS) {
            getLogger().info("PlayerInteractEntity: " + player.getName() + " " + lock +
                             (bypassing ? " bypass" : "") + " " + frame.getItem());
        }

        MetadataValue actionMeta = getMetadata(player, ACTION_KEY);
        if (actionMeta != null) {
            performCommandAction(player, bypassing, lock, (Map<String, Object>) actionMeta.value());
            event.setCancelled(true);

            boolean persistent = (getMetadata(player, PERSIST_KEY) != null);
            if (!persistent) {
                player.removeMetadata(ACTION_KEY, this);
            }
            return;
        }

        // No command action. Handle players right clicking frames.
        if (!bypassing) {
            if (frame.getItem() == null || frame.getItem().getType() == Material.AIR) {
                // Player placing an item. Note: isEmpty() is for vehicles.
                if (!lock.canBeAccessedBy(player)) {
                    event.setCancelled(true);
                    accessDeniedMessage(player, "access", lock);
                }
            } else {
                // Player rotating an item.
                if (!lock.canBeRotatedBy(player)) {
                    event.setCancelled(true);
                    accessDeniedMessage(player, "rotate", lock);
                }
            }
        }
    } // onPlayerInteractEntity

    // ------------------------------------------------------------------------
    /**
     * Don't let players or projectiles knock items out of owned frames.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ITEM_FRAME) {
            return;
        }

        ItemFrame frame = (ItemFrame) entity;
        ItemLock lock = new ItemLock(frame);

        // If a player directly left clicks, message them when denied.
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            boolean bypassing = (getMetadata(player, BYPASS_KEY) != null);
            if (!bypassing && !lock.canBeAccessedBy(player)) {
                event.setCancelled(true);
                accessDeniedMessage(player, "access", lock);
            }
        } else {
            // Projectiles can't remove items from owned frames.
            if (lock.isOwned()) {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Don't let item frames that have an owner break, except when broken
     * directly by:
     * <ul>
     * <li>their owner or members,</li>
     * <li>someone in bypass mode.</li>
     * </ul>
     */
    @EventHandler(ignoreCancelled = true)
    protected void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.ITEM_FRAME) {
            return;
        }

        ItemFrame frame = (ItemFrame) event.getEntity();
        ItemLock lock = new ItemLock(frame);
        Player player = (event.getRemover() instanceof Player) ? (Player) event.getRemover() : null;
        boolean bypassing = (player != null && getMetadata(player, BYPASS_KEY) != null);

        if (event.getCause() != HangingBreakEvent.RemoveCause.PHYSICS &&
            !bypassing && !lock.permits(player)) {
            event.setCancelled(true);
            if (player != null) {
                accessDeniedMessage(player, "break", lock);
            }
        }
        // Potential to log dropped item here if floating frames not enabled.
    }

    // ------------------------------------------------------------------------
    /**
     * Handle removal of the block supporting a locked item frame
     * (RemoveCause.PHYSICS) according to whether floating frames are allowed in
     * the configuration.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onHangingBreak(HangingBreakEvent event) {
        if (event.getEntity().getType() != EntityType.ITEM_FRAME) {
            return;
        }

        ItemFrame frame = (ItemFrame) event.getEntity();
        ItemLock lock = new ItemLock(frame);
        if (event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS &&
            CONFIG.FLOATING_FRAMES && lock.isOwned()) {
            event.setCancelled(true);
        }
        // Potential to log dropped item here if floating frames not enabled.
    }

    // ------------------------------------------------------------------------
    /**
     * When a player places an item frame, if auto-locking is enabled, lock it
     * to the player. In addition, if auto region locking is enabled, add the
     * most specific region to the lock.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onHangingPlace(HangingPlaceEvent event) {
        if (event.getEntity().getType() != EntityType.ITEM_FRAME ||
            !CONFIG.AUTO_LOCK) {
            return;
        }

        ItemFrame frame = (ItemFrame) event.getEntity();
        ItemLock lock = new ItemLock(frame);
        Player player = event.getPlayer();

        // If the player runs /ilock before placing, use owner/region from that.
        MetadataValue actionMeta = getMetadata(player, ACTION_KEY);
        if (actionMeta != null) {
            Map<String, Object> args = (Map<String, Object>) actionMeta.value();
            if (args.get("command").equals("lock")) {
                PermissionChange permissions = (PermissionChange) args.get("permissions");
                doLock(player, lock, permissions);
            }

            boolean persistent = (getMetadata(player, PERSIST_KEY) != null);
            if (!persistent) {
                player.removeMetadata(ACTION_KEY, this);
            }

        } else {
            // Infer permissions from context.
            Set<String> regions = null;
            String regionName = null;

            if (CONFIG.AUTO_LOCK_REGION) {
                regions = getMostSpecificRegionNames(frame.getLocation());
                if (regions.size() == 1) {
                    regionName = regions.stream().findFirst().get();
                }
            }

            doLock(player, lock, new PermissionChange(player, regionName, PermissionGroup.MEMBERS, PermissionGroup.NOBODY));
            if (regions != null && regions.size() > 1) {
                player.sendMessage(ChatColor.GOLD + "Multiple regions exist here: " +
                                   regions.stream().map((r) -> ChatColor.YELLOW + r)
                                   .collect(Collectors.joining(ChatColor.GOLD + ", ")));
            }
        }
    } // onHangingPlace

    // ------------------------------------------------------------------------
    /**
     * Perform an action initiated by a command.
     * 
     * @param player the player interacting with the ItemFrame.
     * @param bypassing true if the player is currently bypassing access
     *        permission checks.
     * @param lock the ItemLock.
     * @param args a map of objects describing the action.
     */
    protected void performCommandAction(Player player, boolean bypassing, ItemLock lock, Map<String, Object> args) {
        String action = (String) args.get("command");
        if (action.equals("lock")) {
            if (!lock.isOwned()) {
                PermissionChange permissions = (PermissionChange) args.get("permissions");
                doLock(player, lock, permissions);
            } else {
                accessDeniedMessage(player, "lock", lock);
            }

        } else if (action.equals("modify")) {
            if (bypassing || lock.permits(player)) {
                PermissionChange permissions = (PermissionChange) args.get("permissions");
                doLock(player, lock, permissions);
            } else {
                accessDeniedMessage(player, "modify the permissions of", lock);
            }

        } else if (action.equals("unlock")) {
            if (bypassing || lock.permits(player)) {
                lock.unlock();
                player.sendMessage(ChatColor.GOLD + "Item frame unlocked!");
            } else {
                accessDeniedMessage(player, "unlock", lock);
            }

        } else if (action.equals("info")) {
            OfflinePlayer owner = lock.getOwner();
            String region = lock.getRegionName();
            String ownerDescription = (owner != null) ? ChatColor.YELLOW + owner.getName()
                                                      : ChatColor.LIGHT_PURPLE + "<nobody>";
            String regionDescription = (region != null) ? ChatColor.YELLOW + region
                                                        : ChatColor.LIGHT_PURPLE + "<none>";
            player.sendMessage(ChatColor.GOLD + "Owner: " + ownerDescription +
                               ChatColor.GOLD + ", region: " + regionDescription);
            player.sendMessage(ChatColor.GOLD + "Access group: " + lock.getAccessGroup().formatted() +
                               ChatColor.GOLD + ", rotate group: " + lock.getRotateGroup().formatted());
        }
    } // performCommandAction

    // ------------------------------------------------------------------------
    /**
     * Lock or modify permissions of a frame and message the player who
     * performed the action.
     * 
     * @param player the player creating the lock.
     * @param lock the lock information.
     * @param permissions the permissions (must be non-null).
     */
    protected void doLock(Player player, ItemLock lock, PermissionChange permissions) {
        if (lock.isOwned()) {
            // Modify existing lock.
            if (permissions.getOwner() != null) {
                lock.setOwnerUuid(permissions.getOwner().getUniqueId());
            }
        } else {
            // Lock an unlocked frame.
            OfflinePlayer newOwner = (permissions.getOwner() != null ? permissions.getOwner() : player);
            lock.setOwnerUuid(newOwner.getUniqueId());
        }

        if (permissions.getRegionName() != null) {
            if (permissions.getRegionName().equals("-")) {
                lock.setRegionName(null);
            } else {
                lock.setRegionName(permissions.getRegionName());
            }
        }

        if (permissions.getAccessGroup() != null) {
            lock.setAccessGroup(permissions.getAccessGroup());
        }

        if (permissions.getRotateGroup() != null) {
            lock.setRotateGroup(permissions.getRotateGroup());
        }

        StringBuilder s = new StringBuilder();
        s.append(ChatColor.GOLD).append("That item frame now belongs to ");
        s.append(getOwnerAndRegionString(player, lock));
        s.append(ChatColor.GOLD).append(".");
        player.sendMessage(s.toString());
        player.sendMessage(ChatColor.GOLD + "Access group: " + lock.getAccessGroup().formatted() +
                           ChatColor.GOLD + ", rotate group: " + lock.getRotateGroup().formatted());
    }

    // ------------------------------------------------------------------------
    /**
     * Show a standard error message when a player tries to access an item frame
     * that they are not allowed to.
     * 
     * @param player the player.
     * @param verb a word describing the action performed; to be interpolated
     *        into the message.
     * @param lock information about the item frame lock.
     */
    protected void accessDeniedMessage(Player player, String verb, ItemLock lock) {
        player.sendMessage(ChatColor.RED + "You can't " + verb + " that item frame!");
        player.sendMessage(ChatColor.GOLD + "It is owned by " + getOwnerAndRegionString(player, lock) +
                           ChatColor.GOLD + ".");
        player.sendMessage(ChatColor.GOLD + "Access group: " + lock.getAccessGroup().formatted() +
                           ChatColor.GOLD + ", rotate group: " + lock.getRotateGroup().formatted());
    }

    // ------------------------------------------------------------------------
    /**
     * Send a command sender an error message about invalid command arguments.
     * 
     * @param sender the command sender.
     * @param commandName the command name.
     */
    protected void invalidArgumentsMessage(CommandSender sender, String commandName) {
        sender.sendMessage(ChatColor.RED + "Invalid arguments. Try \"/" + commandName + " help\".");
    }

    // ------------------------------------------------------------------------
    /**
     * Get a coloured string listing the owner and region name of an owned
     * frame, for use in messages.
     * 
     * @param player the player.
     * @param lock the lock information; must have an owner.
     * @return the name of the owner, and region, if applicable.
     */
    protected String getOwnerAndRegionString(Player player, ItemLock lock) {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.YELLOW).append(lock.isOwnedBy(player) ? "you" : lock.getOwner().getName());
        if (lock.getRegionName() != null) {
            s.append(ChatColor.GOLD).append(", region ");
            s.append(ChatColor.YELLOW).append(lock.getRegionName());
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Compute the names of the most specific WorldGuard regions at a location.
     * 
     * If a child region overlaps its parent at a location, then the parent is
     * omitted from the result, since the child includes all the parent's owners
     * and members (and then some).
     * 
     * @param loc the location.
     * @return a set of the names of all unrelated (by ancestry) regions at the
     *         location.
     */
    protected Set<String> getMostSpecificRegionNames(Location loc) {
        RegionManager manager = getWorldGuard().getRegionManager(loc.getWorld());
        ApplicableRegionSet applicableRegions = manager.getApplicableRegions(loc);
        Set<ProtectedRegion> distinctRegions = applicableRegions.getRegions();
        for (ProtectedRegion region : applicableRegions.getRegions()) {
            ProtectedRegion ancestor = region.getParent();
            while (ancestor != null) {
                distinctRegions.remove(ancestor);
                ancestor = ancestor.getParent();
            }
        }
        return new TreeSet<String>(distinctRegions.stream().map((r) -> r.getId()).collect(Collectors.toList()));
    }

    // ------------------------------------------------------------------------
    /**
     * Get the metadata with the specified key that was set by this plugin.
     * 
     * @param holder the holder of the metadata.
     * @param key the key.
     * @return matching metadata that was set by this plugin, or null if not
     *         found.
     */
    protected MetadataValue getMetadata(Metadatable holder, String key) {
        for (MetadataValue meta : holder.getMetadata(key)) {
            if (meta.getOwningPlugin() == this) {
                return meta;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Metadata key for metadata recording the name and arguments of the
     * player's last item frame action.
     */
    private static final String ACTION_KEY = "action";

    /**
     * Metadata key for the metadata signifying that the command metadata is
     * persistent.
     */
    private static final String PERSIST_KEY = "persist";

    /**
     * Metadata key for the metadata signifying that the player can bypass
     * ownership/region membership checks.
     */
    private static final String BYPASS_KEY = "bypass";

    // ------------------------------------------------------------------------
    /**
     * The WorldGuard plugin.
     */
    private WorldGuardPlugin _worldGuard;
} // class ItemLocker