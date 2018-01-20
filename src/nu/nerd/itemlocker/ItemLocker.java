package nu.nerd.itemlocker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

        File playersFile = new File(getDataFolder(), PLAYERS_FILE);
        _playerConfig = YamlConfiguration.loadConfiguration(playersFile);

        Bukkit.getPluginManager().registerEvents(this, this);

        _worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        for (PlayerState state : _state.values()) {
            state.save(_playerConfig);
        }

        try {
            _playerConfig.save(new File(getDataFolder(), PLAYERS_FILE));
        } catch (IOException ex) {
            getLogger().warning("Unable to save player data: " + ex.getMessage());
        }
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

            if (isBypassing(player)) {
                player.sendMessage(ChatColor.GOLD + "You will no longer bypass frame/stand permission checks.");
                player.removeMetadata(BYPASS_KEY, this);
            } else {
                player.sendMessage(ChatColor.GOLD + "You can now bypass frame/stand permission checks!");
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
                player.sendMessage(ChatColor.GOLD + "Your frame/stand actions will no longer persist.");
                player.removeMetadata(PERSIST_KEY, this);

                // Clear the previous action.
                player.removeMetadata(ACTION_KEY, this);
            } else {
                player.sendMessage(ChatColor.GOLD + "Your frame/stand actions will now persist.");
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
                    sender.sendMessage(ChatColor.GREEN + "The next frame or stand you place or right click will be locked to " +
                                       ChatColor.YELLOW + ownerMessage + ChatColor.GREEN + ".");

                    // Apply the player's /idefault permissions, but only if
                    // locking their own item frame.
                    if (permissions.getOwner() == null || permissions.getOwner() == player) {
                        PlayerState state = getState(player);
                        PermissionChange defaultPermissions = state.asPermissionChange();
                        permissions = defaultPermissions.overriddenBy(permissions);
                    }
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
                    sender.sendMessage(ChatColor.GREEN + "Right click a frame or stand to change its permissions.");
                    commandArgs.put("permissions", permissions);
                }

            } else if (commandName.equals("iunlock")) {
                if (args.length > 0) {
                    invalidArgumentsMessage(sender, commandName);
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Right click a frame or stand to unlock it.");

            } else if (commandName.equals("iinfo")) {
                if (args.length > 0) {
                    invalidArgumentsMessage(sender, commandName);
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Right click a frame or stand to see who owns it.");

            } else if (commandName.equals("idefault")) {
                Object parsed = PermissionChange.parse(player, args);
                if (parsed instanceof String) {
                    sender.sendMessage(ChatColor.RED + (String) parsed);
                    return true;
                } else {
                    PermissionChange permissions = (PermissionChange) parsed;
                    if (permissions.getOwner() != null && !permissions.getOwner().equals(player)) {
                        sender.sendMessage(ChatColor.RED + "You cannot set another player as the default owner of your frames and stands.");
                        return true;
                    }

                    PlayerState state = getState(player);
                    state.setDefaultRegionName(permissions.getRegionName());
                    if (permissions.getAccessGroup() != null) {
                        state.setDefaultAccessGroup(permissions.getAccessGroup());
                    }
                    if (permissions.getRotateGroup() != null) {
                        state.setDefaultRotateGroup(permissions.getRotateGroup());
                    }

                    sender.sendMessage(ChatColor.GREEN + "Your default frame/stand permissions are now:");
                    StringBuilder message = new StringBuilder();
                    message.append(ChatColor.GOLD).append("Region: ");
                    message.append(getRegionString(state.getDefaultRegionName()));

                    PermissionGroup access = (state.getDefaultAccessGroup() == null) ? PermissionGroup.MEMBERS : state.getDefaultAccessGroup();
                    PermissionGroup rotate = (state.getDefaultRotateGroup() == null) ? PermissionGroup.MEMBERS : state.getDefaultRotateGroup();
                    message.append(ChatColor.GOLD).append(", access: ").append(access.formatted());
                    message.append(ChatColor.GOLD).append(", rotate: ").append(rotate.formatted());
                    sender.sendMessage(message.toString());
                    return true;
                }
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
     * On join, allocate each player a {@link PlayerState} instance.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        _state.put(player.getName(), new PlayerState(player, _playerConfig));
    }

    // ------------------------------------------------------------------------
    /**
     * On quit, forget the {@link PlayerState}.
     */
    @EventHandler(ignoreCancelled = true)
    protected void onPlayerQuit(PlayerQuitEvent event) {
        PlayerState state = _state.remove(event.getPlayer().getName());
        state.save(_playerConfig);
    }

    // ------------------------------------------------------------------------
    /**
     * Record the details of a player placing an armour stand so that the entity
     * can be locked to the player when it spawns.
     * 
     * There is no event in the Bukkit API that records which player spawned an
     * armour stand.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    protected void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
            event.getMaterial() == Material.ARMOR_STAND) {
            Block standBlock = event.getClickedBlock().getRelative(event.getBlockFace());
            Location loc = standBlock.getLocation().add(0.5, 0, 0.5);
            if (CONFIG.DEBUG_EVENTS) {
                getLogger().info("onPlayerInteract: placed stand on " +
                                 standBlock.getType() + ":" + (int) standBlock.getData() + " at Y " + loc.getBlockY());
            }
            _placements.add(new StandPlacement(event.getPlayer(), loc));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When an armour stand spawns, find the corresponding
     * {@link StandPlacement} so that we can lock the stand to the player that
     * placed it.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    protected void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ARMOR_STAND &&
            event.getSpawnReason() == SpawnReason.DEFAULT) {
            if (CONFIG.DEBUG_EVENTS) {
                getLogger().info("onCreatureSpawn: stand spawn at Y " + event.getLocation().getBlockY());
            }
            for (int i = _placements.size() - 1; i >= 0; --i) {
                StandPlacement placement = _placements.get(i);
                Location loc = event.getLocation();
                if (placement.matches(loc)) {
                    doLockOnPlace(placement.getPlayer(), event.getEntity());
                    _placements.remove(i);
                } else if (placement.isOutdated(loc)) {
                    _placements.remove(i);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking item frames.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.ITEM_FRAME) {
            handleInteraction(event);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking armour stands.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.ARMOR_STAND) {
            handleInteraction(event);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Don't let players or projectiles knock items out of owned frames or
     * stands.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.ITEM_FRAME &&
            entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }

        ItemLock lock = new ItemLock(entity);

        // If a player directly left clicks, message them when denied.
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            boolean bypassing = isBypassing(player);

            if (CONFIG.DEBUG_EVENTS) {
                getLogger().info("onEntityDamageByEntity: " + player.getName() + " " + lock +
                                 (bypassing ? " bypass" : "") + " " +
                                 lock.getItems().stream().map((i) -> i.toString()).collect(Collectors.joining(", ")));
            }

            if (!bypassing && !lock.canBeAccessedBy(player)) {
                event.setCancelled(true);
                accessDeniedMessage(player, "access", lock);
            }
        } else {
            // Projectiles can't remove items from owned frames and stands.
            if (lock.isOwned()) {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent damage to armour stands by anything that is not a player attack.
     * 
     * That includes fire, lava and projectiles. Player attacks are handled in
     * {@link ItemLocker#onEntityDamageByEntity()}.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ARMOR_STAND && event.getCause() != DamageCause.ENTITY_ATTACK) {
            ItemLock lock = new ItemLock(entity);
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
        if (!lock.isOwned()) {
            return;
        }

        Player player = (event.getRemover() instanceof Player) ? (Player) event.getRemover() : null;
        if (player == null) {
            event.setCancelled(true);
        } else {
            if (!isBypassing(player) && !lock.canBeAccessedBy(player)) {
                event.setCancelled(true);
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
        if (lock.isOwned()) {
            if (event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS) {
                if (CONFIG.FLOATING_FRAMES) {
                    event.setCancelled(true);
                }
            } else {
                if (event.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
                    event.setCancelled(true);
                }
            }
        }
        // Potential to log dropped item here if floating frames not enabled.
    }

    // ------------------------------------------------------------------------
    /**
     * When a player places an item frame, if auto-locking is enabled, lock it
     * to the player.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    protected void onHangingPlace(HangingPlaceEvent event) {
        if (event.getEntity().getType() == EntityType.ITEM_FRAME) {
            doLockOnPlace(event.getPlayer(), event.getEntity());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If a player runs `/ilock` before placing a frame or stand, or if
     * automatic locking is enabled, lock the frame/stand.
     * 
     * If auto region locking is enabled, add the most specific region to the
     * lock.
     */
    protected void doLockOnPlace(Player player, Entity entity) {
        ItemLock lock = new ItemLock(entity);

        // If the player runs /ilock before placing, use owner/region from
        // that.
        boolean handled = false;
        MetadataValue actionMeta = getMetadata(player, ACTION_KEY);
        if (actionMeta != null) {
            Map<String, Object> args = (Map<String, Object>) actionMeta.value();
            if (args.get("command").equals("lock")) {
                PermissionChange permissions = (PermissionChange) args.get("permissions");
                doLock(player, lock, permissions);
                handled = true;

                boolean persistent = (getMetadata(player, PERSIST_KEY) != null);
                if (!persistent) {
                    player.removeMetadata(ACTION_KEY, this);
                }
            }

        }

        if (!handled && CONFIG.AUTO_LOCK) {
            PlayerState state = getState(player);
            String regionName = state.getDefaultRegionName();
            boolean doRegionInference = (CONFIG.AUTO_LOCK_REGION && regionName == null);
            Set<String> regions = null;

            if (doRegionInference) {
                // Infer region name from context.
                regions = getMostSpecificRegionNames(entity.getLocation());
                if (regions.size() == 1) {
                    regionName = regions.stream().findFirst().get();
                }
            }

            PermissionChange defaultLock = new PermissionChange(player, regionName, PermissionGroup.MEMBERS, PermissionGroup.MEMBERS);
            doLock(player, lock, defaultLock.overriddenBy(state.asPermissionChange()));
            if (doRegionInference && regions != null && regions.size() > 1) {
                player.sendMessage(ChatColor.GOLD + "Multiple regions exist here: " +
                                   regions.stream().map((r) -> ChatColor.YELLOW + r)
                                   .collect(Collectors.joining(ChatColor.GOLD + ", ")));
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking item frames and armour stands.
     * 
     * The player's current command and /ipersist state is extracted from their
     * metadata.
     * 
     * For item frames, rotation is distinguished from item access according to
     * whether the frame is empty or not.
     */
    protected void handleInteraction(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        ItemLock lock = new ItemLock(entity);
        Player player = event.getPlayer();
        boolean bypassing = isBypassing(player);

        if (CONFIG.DEBUG_EVENTS) {
            getLogger().info("handleInteraction: " + player.getName() + " " + lock +
                             (bypassing ? " bypass" : "") + " " +
                             lock.getItems().stream().map((i) -> i.toString()).collect(Collectors.joining(", ")));
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

        // No command action. Handle players right clicking.
        if (!bypassing) {
            if (lock.isEmpty() || !lock.isFrame()) {
                // Player placing an item.
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
    }

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
                player.sendMessage(ChatColor.GOLD + "That " + lock.getEntityType() + " is now unlocked!");
            } else {
                accessDeniedMessage(player, "unlock", lock);
            }

        } else if (action.equals("info")) {
            OfflinePlayer owner = lock.getOwner();
            String region = lock.getRegionName();
            String ownerDescription = (owner != null) ? ChatColor.YELLOW + owner.getName()
                                                      : ChatColor.LIGHT_PURPLE + "<nobody>";
            String regionDescription = getRegionString(region);
            player.sendMessage(ChatColor.GOLD + "Owner: " + ownerDescription +
                               ChatColor.GOLD + ", region: " + regionDescription);
            permissionMessage(player, lock);
        }
    } // performCommandAction

    // ------------------------------------------------------------------------
    /**
     * Lock or modify permissions of a frame or stand and message the player who
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
            // Lock an unlocked entity.
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
        s.append(ChatColor.GOLD).append("That " + lock.getEntityType() + " now belongs to ");
        s.append(getOwnerAndRegionString(player, lock));
        s.append(ChatColor.GOLD).append(".");
        player.sendMessage(s.toString());
        permissionMessage(player, lock);
    }

    // ------------------------------------------------------------------------
    /**
     * Show a standard error message when a player tries to access a frame or
     * stand that they are not allowed to.
     * 
     * @param player the player.
     * @param verb a word describing the action performed; to be interpolated
     *        into the message.
     * @param lock information about the lock.
     */
    protected void accessDeniedMessage(Player player, String verb, ItemLock lock) {
        player.sendMessage(ChatColor.RED + "You can't " + verb + " that " + lock.getEntityType() + "!");
        player.sendMessage(ChatColor.GOLD + "It is owned by " + getOwnerAndRegionString(player, lock) +
                           ChatColor.GOLD + ".");
        permissionMessage(player, lock);
    }

    // ------------------------------------------------------------------------
    /**
     * Send a player a message detailing the permission groups of a lock.
     * 
     * @param player the player.
     * @param lock the lock.
     */
    protected void permissionMessage(Player player, ItemLock lock) {
        StringBuilder s = new StringBuilder();
        s.append(ChatColor.GOLD).append("Access group: ").append(lock.getAccessGroup().formatted());
        if (lock.isFrame()) {
            s.append(ChatColor.GOLD).append(", rotate group: ").append(lock.getRotateGroup().formatted());
        }
        player.sendMessage(s.toString());
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
     * frame/stand, for use in messages.
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
     * Return a coloured string describing a region name.
     * 
     * Rather than omitting the region, a null region is shown as "<none>".
     * 
     * @param region the name of the region.
     * @return a description of the region for presentation.
     */
    protected String getRegionString(String region) {
        return (region != null) ? ChatColor.YELLOW + region
                                : ChatColor.LIGHT_PURPLE + "<none>";
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
     * Return true if the player can currently bypass permission checks.
     * 
     * If the player had bypass metadata set and the permission to set it has
     * since been revoked, the metadata is removed. This prevents mods from
     * enabling bypass mode and retaining it when they leave ModMode.
     * 
     * @param player the player.
     * @return true if the player can currently bypass permission checks.
     */
    protected boolean isBypassing(Player player) {
        if (!player.hasPermission("itemlocker.bypass")) {
            player.removeMetadata(BYPASS_KEY, this);
        }

        return (getMetadata(player, BYPASS_KEY) != null);
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
     * Return the {@link PlayerState} for the specified player.
     *
     * @param player the player.
     * @return the {@link PlayerState} for the specified player.
     */
    protected PlayerState getState(Player player) {
        return _state.get(player.getName());
    }

    // ------------------------------------------------------------------------
    /**
     * Name of players file.
     */
    private static final String PLAYERS_FILE = "players.yml";

    /**
     * Metadata key for metadata recording the name and arguments of the
     * player's last frame/stand action.
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

    /**
     * List of {@link StandPlacement} details of armour stands, used to work out
     * which player spawned a stand.
     * 
     * These are only retained for the brief time between the
     * PlayerInteractEvent where the player places the stand and the subsequent
     * CreatureSpawnEvent when the stand entity spawns.
     */
    private final ArrayList<StandPlacement> _placements = new ArrayList<>();

    /**
     * Map from Player name to {@link PlayerState} instance.
     *
     * A Player's PlayerState exists only for the duration of a login.
     */
    private final HashMap<String, PlayerState> _state = new HashMap<>();

    /**
     * Configuration file for per-player settings.
     */
    private YamlConfiguration _playerConfig;
} // class ItemLocker