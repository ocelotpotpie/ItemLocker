package nu.nerd.itemlocker;

import org.bukkit.ChatColor;

// ----------------------------------------------------------------------------
/**
 * An enumeration type representing the three possible groups of players that
 * can affect an item frame.
 */
public enum PermissionGroup {
    /**
     * Nobody can alter the frame.
     */
    NOBODY(ChatColor.RED, '-'),

    /**
     * The owner and region owners/members can alter the frame.
     */
    MEMBERS(ChatColor.YELLOW, '+'),

    /**
     * Any player can alter the frame.
     */
    PUBLIC(ChatColor.GREEN, '*');

    // ----------------------------------------------------------------------------
    /**
     * Return the PermissionGroup corresponding to the specified code character.
     * 
     * @param code the code character.
     * @return the corresponding PermissionGroup, or null if the code is not a
     *         valid option.
     */
    public static PermissionGroup fromCode(char code) {
        switch (code) {
        case '-':
            return NOBODY;
        case '+':
            return MEMBERS;
        case '*':
            return PUBLIC;
        default:
            return null;
        }
    }

    // ----------------------------------------------------------------------------
    /**
     * Return the code character corresponding to the group.
     * 
     * @return the code character corresponding to the group.
     */
    public char getCode() {
        return _code;
    }

    // ----------------------------------------------------------------------------
    /**
     * Return the list of all valid code characters as a String.
     * 
     * @return the list of all valid code characters as a String.
     */
    public static String getAllCodes() {
        return _allCodes;
    }

    // ----------------------------------------------------------------------------
    /**
     * Return this group formatted for presentation in user messages.
     * 
     * @return this group formatted for presentation in user messages.
     */
    public String formatted() {
        return _colour + toString().toLowerCase() +
               ChatColor.WHITE + " (" + ChatColor.YELLOW + getCode() +
               ChatColor.WHITE + ")";
    }

    // ----------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param code the code character corresponding to the group.
     */
    private PermissionGroup(ChatColor colour, char code) {
        _colour = colour;
        _code = code;
    }

    // ----------------------------------------------------------------------------
    /**
     * Initialise a list of all code characters (stored as a String).
     */
    private static String _allCodes;
    static {
        StringBuilder s = new StringBuilder();
        for (PermissionGroup group : values()) {
            s.append(group.getCode());
        }
        _allCodes = s.toString();
    }

    /**
     * The colour to show this group in player messages.
     */
    private ChatColor _colour;

    /**
     * The code character corresponding to the group.
     */
    private char _code;
} // class PermissionGroup