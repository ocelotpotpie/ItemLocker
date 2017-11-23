package nu.nerd.itemlocker;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

// ----------------------------------------------------------------------------
/**
 * Transient, per-player state, created on join and removed when the player
 * leaves.
 */
public class PlayerState {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param player the player.
     * @param config the configuration from which player preferences are loaded.
     */
    public PlayerState(Player player, YamlConfiguration config) {
        _player = player;
        load(config);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the default permissions as a PermissionChange instance.
     * 
     * @return the default permissions as a PermissionChange instance.
     */
    public PermissionChange asPermissionChange() {
        return new PermissionChange(_player, getDefaultRegionName(), getDefaultAccessGroup(), getDefaultRotateGroup());
    }

    // ------------------------------------------------------------------------
    /**
     * Set the default region name to use when locking.
     * 
     * @param name the region name; use "-" to signify no region.
     */
    public void setDefaultRegionName(String name) {
        _defaultRegionName = name;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the default region name to use when locking.
     * 
     * A non-null region name will suppress region inference if none is
     * specified in the /ilock command.
     * 
     * @return the default region name to use when locking.
     */
    public String getDefaultRegionName() {
        return _defaultRegionName;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the default access group to use when locking.
     * 
     * @param group the group.
     */
    public void setDefaultAccessGroup(PermissionGroup group) {
        _defaultAccessGroup = group;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the default access permission group to use when locking.
     * 
     * @return the default access permission group to use when locking.
     */
    public PermissionGroup getDefaultAccessGroup() {
        return _defaultAccessGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the default rotate group to use when locking.
     * 
     * @param group the group.
     */
    public void setDefaultRotateGroup(PermissionGroup group) {
        _defaultRotateGroup = group;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the default rotate permission group to use when locking.
     * 
     * @return the default rotate permission group to use when locking.
     */
    public PermissionGroup getDefaultRotateGroup() {
        return _defaultRotateGroup;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this player's preferences to the specified configuration.
     *
     * @param config the configuration to update.
     */
    public void save(YamlConfiguration config) {
        ConfigurationSection section = config.createSection(_player.getUniqueId().toString());
        section.set("name", _player.getName());
        section.set("region", _defaultRegionName);
        if (_defaultAccessGroup != null) {
            section.set("access", _defaultAccessGroup.toString());
        }
        if (_defaultRotateGroup != null) {
            section.set("rotate", _defaultRotateGroup.toString());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Load the Player's preferences from the specified configuration
     *
     * @param config the configuration from which player preferences are loaded.
     */
    protected void load(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        if (section == null) {
            section = config.createSection(_player.getUniqueId().toString());
        }
        _defaultRegionName = section.getString("region");
        _defaultAccessGroup = parsePermissionGroup(section.getString("access"));
        _defaultRotateGroup = parsePermissionGroup(section.getString("rotate"));
    }

    // ------------------------------------------------------------------------
    /**
     * Parse a permission group setting loaded from the configuration.
     * 
     * @param setting the group as a string.
     * @return the corresponding PermissionGroup; return null for a null (unset)
     *         setting.
     */
    protected static PermissionGroup parsePermissionGroup(String setting) {
        return (setting == null) ? null : PermissionGroup.valueOf(setting);
    }

    // ------------------------------------------------------------------------
    /**
     * The player's name.
     */
    protected Player _player;

    /**
     * Default region name.
     */
    protected String _defaultRegionName;

    /**
     * Default access permission group; null to signify use the global default.
     */
    protected PermissionGroup _defaultAccessGroup;

    /**
     * Default rotate permission group; null to signify use the global default.
     */
    protected PermissionGroup _defaultRotateGroup;
} // class PlayerState