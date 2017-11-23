ItemLocker
==========
Locks item frames.

Features
--------
 * Allows item frames and armor stands to be locked to a single player, and
   optionally the set of players who are owners and members of a WorldGuard
   region and its parents.
 * Automatically locks frames and stands (if configured).
 * Automatically infers the WorldGuard region to associate with a frame/stand
   (if configured).   
 * Allows control over which players can put items into and take items from a
   frame or stand.
 * Allows control over which players can rotate frames.
 * Allows the owners of frames and stands to deny themselves these
   put/take/rotate permissions so that they cannot accidentally alter them.
 * Prevents explosions, projectiles and obstruction from damaging locked frames
   and stands.
 * Allows locked frames to float in the air rather than break when their
   supporting block is removed (if configured).
 * Allows commands to persist (be repeated).
 * Allows staff to bypass locks and change permissions.


Permission Model
----------------
Every locked frame or stand has an _owner_ (the player who ran `/ilock`) and
can optionally have an associated WorldGuard region.

The _members_ of a frame or stand are defined as the aforementioned _owner_ and
all _owners_ and _members_ of the WorldGuard region and its parent region(s),
if it has them.

All _members_ of a frame/stand have the same permissions on it. They can:

 * Change the `access` and `rotate` permission groups of an item frame, or
   the `access` permission group of an armor stand.
 * Change the associated region.
 * Break an armour stand or an empty item frame (unlocking it).
 * Unlock the frame or stand.

If a member other than the owner breaks a frame or stand and places it, they
become its new owner.


Access and Rotate Groups
------------------------
Every frame records two _groups_. (Note: these are _not_ the same as Bukkit
permission groups.)

 1. The _access_ group, which can be abbreviated in commands as _a_, is the set
    of players that can put into and take from the frame. 
 1. The rotate group, which can be abbreviated in commands as _r_, is the set
    of players that can rotate the frame.
    
Armor stands only have an _access_ group.

Each group can be one of three settings: 

 1. _nobody_, signified by **-** in commands.
 1. _members_, signified by **+** in commands.
 1. _public_, signified by **\*** in commands.
 
For the _access_ group, the default setting for new locks is to allow the 
owner and region, i.e. `+access` (`+a`).

For the _rotate_ group, the default setting for new locks is to allow the 
owner and region, i.e. `+rotate` (`+r`).


Region Inference
----------------
If automatic locking and region inference are both enabled in the configuration,
then when you place the frame or stand, it will have its region set
automatically according to the following rules:

 * If child regions overlap their parents at the frame/stand location, the
   deepest descendent child takes precedence.
   
 * If there are multiple unrelated regions at the location, the desired
   region is ambiguous, so no region is selected.

Note that if you run `/ilock` before placing the frame or stand, then the 
arguments of that command are deemed to override the inferred region. If you 
don't specify a region in the `/ilock` arguments (or specify `r:-`), no 
region will be set on the frame/stand.


Per-Player Defaults
-------------------
Using the `/idefault` command, a player can set a default region, access
group and rotate group that will be applied to every frame or stand when it
is locked. If the frame or stand is locked automatically when placed, then
the per-player defaults simply override the global defaults (inferred region,
members access, members rotate). The parameters of the `/idefault` command
are the same as those of `/imodify`, except that you can not set a default
owner (player name).

If the `/ilock` command is run immediately prior to placing a frame or stand,
then the region and groups (if any) given to `/ilock` take precedence over
the player's defaults.

The default region set by `/idefault` takes precedence over any region
automatically inferred from the location where the frame or stand is placed.
For example, `/idefault r:myregion` ensures that all placed frames/stands
will have the "myregion" region, by default. You can use `/idefault r:-` to
specify that they will have no region, even where region inference would
normally come into play.

If you want to re-enable region inference, simply omit the region argument to 
the `/idefault` command. Just running `/idefault` will clear the default 
region without changing the default access or rotate groups set by the player.

The global default permissions of frames and stands are equivalent to
`/idefault +access +rotate`.

To allow all subsequent frames that you place to be rotated by anyone on the
server unless overridden with `/ilock` or `/imodify`, use 
`/idefault *rotate` or `/idefault *r`.

To prevent everyone on the server, even yourself, from rotating all subsequent
frames that you place, use `/idefault -rotate` or `/idefault -r`.


Locking a Frame or Stand
------------------------
 * If auto-locking is enabled, simply place the item frame or armor stand to 
   lock it.
   * A region may be automatically inferred per _Region Inference_, above.
   * The frame or stand's permissions can be overridden by running `/ilock` 
     before placing it.

 * If auto-locking is _not_ enabled, run `/ilock` and place the frame or stand.

 * If the frame/stand already exists and is not locked, run `/ilock` and right
   click on it to lock it.
 
 * To lock a frame or stand and allow everyone in the _my_town_ region to put 
   and take the item, but not rotate it: `/ilock r:my_town -rotate`
   (Recall that frames are locked with groups `+access` and `+rotate`, by default.)

 * To lock many frames or stands with the same setting, precede that command 
   with `/ipersist`.

 * To lock a frame/stand and let the owner and region members rotate it: 
   `/ilock r:regionname +rotate`

 * To lock a frame and let everyone on the server rotate it but prevent them
   from adding or removing an item: `/ilock -a *r`, or equivalently: `/ilock -access *rotate`


Unlocking a Frame or Stand
--------------------------
 * If a frame is empty and you are a member (including the owner), simply punch it.
   It will break and is then unlocked. Armor stands can be broken without being
   empty, provided you are a member.

 * To unlock a frame or stand, even if it contains an item, run `/iunlock` 
   and right click on it. 

 * To unlock many frames or stands, run `/ipersist` before `/iunlock`, then
   right click on all the frames and stands.


Command Persistence
-------------------
Run `/ipersist` to turn on command persistence. Any command you type will
take effect on all subsequent item frames and stands that you interact with
until you run `/ipersist` again, or the server restarts.


Modifying Frame Permissions
---------------------------
To change the region of a locked frame or stand, run `/imodify r:regionname`.

To remove the region of a locked frame or stand, run `/imodify r:-`.

To make the item in a locked frame or stand accessible to:

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

 * They can bypass permission checks on frame and stands to rotate, access 
   items, and break the frame/stand using the `/ibypass` command. Run the 
   command once to turn on bypass mode. Run it again to turn off bypass mode.
   
 * They can lock a frame or stand on behalf of a player by specifying the 
   player name in `/ilock` arguments, e.g. `/ilock totemo`. The specified 
   player becomes the owner.
   
 * They can modify the region and access groups of a frame or stand, 
   e.g. `/imodify r:pico +a -r`. 
   * To prevent accidental modification of the permissions of player frames
     and stands it will usually be necessary to be in bypass mode when running
     `/imodify` in this way, unless the staff member is already a member of
     the frame/stand.
   
 * They can break the frame or stand. If not a member of it, staff will need to
   enable bypass mode to do this.


Configuration
-------------
| Setting           | Default | Description |
| :------           | :------ | :---------- |
| `debug.config`  | true    | If true, log configuration settings when loaded. |
| `debug.events`  | false   | If true, show extra debug messages in event handlers. |
| `floating-frames` | true  | If true, owned frames will float in the air rather than break when their supporting block is removed. |
| `auto-lock`     | true    | If true, automatically lock frames and stands when placed. | 
| `auto-lock-region` | true | If true, automatically infer the region of automatically locked frames and stands. |


Permissions
-----------
 * `itemlocker.console`:
   * Permission to use commands that require console access.
   * Default: op

 * `itemlocker.bypass`:
   * Permission to bypass ownership checks on frames and stands. They can be unlocked and locked to anyone.
   * Default: op

 * `itemlocker.user`:
   * Permission to use `/ipersist`, `/ilock`, `/imodify`, `/iunlock` and `/iinfo`.
   * Default: op
