ItemLocker
==========
Locks item frames.

Features
--------
 * Allows item frames to be locked to a single player, and optionally the set 
   of players who are owners and members of a WorldGuard region and its parents.
 * Automatically locks frames (if configured).
 * Automatically infers the WorldGuard region to associate with a frame
   (if configured).   
 * Allows control over which players can put items into and take items from a
   frame.
 * Allows control over which players can rotate frames. By default, locked 
   frames cannot be rotated.
 * Allows the owners of frames to deny themselves these put/take/rotate 
   permissions so that they cannot accidentally alter frames.
 * Prevents explosions, projectiles and obstruction from damaging locked frames.
 * Allows locked frames to float in the air rather than break when their
   supporting block is removed (if configured).
 * Allows commands to persist (be repeated).
 * Allows staff to bypass locks and change permissions.


Permission Model
----------------
Every locked frame has an _owner_ (the player who ran `/ilock`) and can
optionally have an associated WorldGuard region.

The _members_ of an item frame are defined as the aforementioned _owner_ and
all _owners_ and _members_ of the WorldGuard region and its parent region(s),
if it has them.

All _members_ of a frame have the same permissions on the frame. They can:

 * Change the `access` and `rotate` permission groups of the frame.
 * Change the associated region.
 * Break an empty frame (unlocking it).
 * Unlock the frame.

If a member other than the owner breaks a frame and places it, they become
the new owner of the frame.


Access and Rotate Groups
------------------------
Every frame records two _groups_. (Note: these are _not_ the same as Bukkit
permission groups.)

 1. The _access_ group, which can be abbreviated in commands as _a_, is the set
    of players that can put into and take from the frame. 
 1. The rotate group, which can be abbreviated in commands as _r_, is the set
    of players that can rotate the frame.

Each group can be one of three settings: 

 1. _nobody_, signified by **-** in commands.
 1. _members_, signified by **+** in commands.
 1. _public_, signified by **\*** in commands.
 
For the _access_ group, the default setting for new locks is to allow the 
owner and region, i.e. `+access` (`+a`).

For the _rotate_ group, the default setting for new locks is to deny everybody, 
i.e. `-rotate` (`-r`).


Region Inference
----------------
If automatic locking and region inference are both enabled in the configuration,
then when you place the frame, it will have its region set automatically
according to the following rules:

 * If child regions overlap their parents at the frame location, the deepest
   descendent child takes precedence.
   
 * If there are multiple unrelated regions at the frame location, the desired
   region is ambiguous, so no region is selected.

Note that if you run `/ilock` before placing the frame, then the arguments of
that command are deemed to override the inferred region. If you don't specify a
region in the `/ilock` arguments (or specify `r:-`), no region will be set on
the frame.


Locking a Frame
---------------
 * If auto-locking is enabled, simply place the frame to lock it.
   * A region may be automatically inferred per _Region Inference_, above.
   * The frame's permissions can be overridden by running `/ilock` before 
     placing it.

 * If auto-locking is _not_ enabled, run `/ilock` and place the frame.

 * If the frame already exists and is not locked, run `/ilock` and right click on
   it to lock it.
 
 * To lock a frame and allow everyone in the _my_town_ region to put and take
   the item, but not rotate it: `/ilock r:my_town`
   (Recall that frames are locked with groups `+access` and `-rotate`, by default.)

 * To lock many frames with the same setting, precede that command with 
   `/ipersist`.

 * To lock a frame and let the owner and region members rotate it: `/ilock r:regionname +rotate`

 * To lock a frame and let everyone on the server rotate it but prevent them
   from adding or removing an item: `/ilock -a *r`, or equivalently: `/ilock -access *rotate`


Unlocking a Frame
-----------------
 * If a frame is empty and you are a member (including the owner), simply punch it.
   It will break and is then unlocked.

 * To unlock a frame, even if it contains an item, run `/iunlock` and right click
   on the frame. 

 * To unlock many frames, run `/ipersist` before `/iunlock`, then right click
   on all the frames.


Command Persistence
-------------------
Run `/ipersist` to turn on command persistence. Any command you type will
take effect on all subsequent item frames that you interact with until you
run `/ipersist` again, or the server restarts.


Modifying Frame Permissions
---------------------------
To change the region of a locked frame, run `/imodify r:regionname`.

To remove the region of a locked frame, run `/imodify r:-`.

To make the item in a locked frame accessible to:

 * _nobody_, not even the owner: `/imodify -a` or `/imodify -access`
 * _members_, region and owner: `/imodify +a` or `/imodify +access`
 * _everybody_ on the server: `/imodify *a` or `/imodify *access`
 
To allow the item in a locked frame to be rotated by:

 * _nobody_, not even the owner: `/imodify -r` or `/imodify -rotate`
 * _members_, region and owner: `/imodify +r` or `/imodify +rotate`
 * _everybody_ on the server: `/imodify *r` or `/imodify *rotate`
 
All of these arguments can be combined in a single command, and are subject to
command persistence.


Player Command Reference
------------------------
Command syntax is described in full by in-game help. Just do `/help ItemLocker`
or run `/help` for a specific command, e.g. `/help ilock`.


Staff Commands
--------------
Staff with the `itemlocker.bypass` permission have additional capabilities:

 * They can bypass permission checks on frames to rotate, access items, and
   break the frame using the `/ibypass` command. Run the command once to
   turn on bypass mode. Run it again to turn off bypass mode.
   
 * They can lock a frame on behalf of a player by specifying the player name
   in `/ilock` arguments, e.g. `/ilock totemo`. The specified player becomes
   the frame owner.
   
 * They can modify the region and access groups of a frame, 
   e.g. `/imodify r:pico +a -r`. 
   * To prevent accidental modification of the permissions of player frames
     (see below) it will usually be necessary to be in bypass mode when running
     `/imodify` in this way, unless the staff member is already a member of
     the frame.
   
 * They can break the frame. If not a member of the frame, staff will need to
   enable `/ibypass` to do this.


Configuration
-------------
| Setting           | Default | Description |
| :------           | :------ | :---------- |
| `debug.config`  | true    | If true, log configuration settings when loaded. |
| `debug.events`  | false   | If true, show extra debug messages in event handlers. |
| `floating-frames` | If true, owned frames will float in the air rather than break when their supporting block is removed. |
| `auto-lock`     | true    | If true, automatically lock frames when placed. | 
| `auto-lock-region` | true | If true, automatically infer the region of automatically locked frames. |


Permissions
-----------
 * `itemlocker.console`:
   * Permission to use commands that require console access.
   * Default: op

 * `itemlocker.bypass`:
   * Permission to bypass ownership checks on frames. Frames can be unlocked and locked to anyone.
   * Default: op

 * `itemlocker.user`:
   * Permission to use `/ipersist`, `/ilock`, `/imodify`, `/iunlock` and `/iinfo`.
   * Default: op
