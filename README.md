ItemLocker
==========
Locks Item frames.

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

 1. The _access_ group (which can be abbreviated in commands as _a_) is the set
    of players that can put into and take from the frame. 
 1. The rotate group (which can be abbreviated in commands as _r_), is the set
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
To lock a frame:

 * If auto-locking is enabled, simply place the frame.
   * A region may be automatically inferred per _Region Inference_, above.
   * The frame's permissions can be overridden by running `/ilock` before 
     placing it.

 * If auto-locking is _not_ enabled, run `/ilock` and place the frame.
 
 * To lock a frame and allow everyone in the _my_town_ region to put and take
   the item, but not rotate it:

    /ilock r:my_town

   (Recall that frames are locked with groups `+access` and `-rotate`, by 
default.)

 * To lock many frames with the same setting, precede that command with 
`/ipersist`.
 * To lock a frame and let the owner and region members rotate it:

    /ilock r:regionname +r

 * To lock a frame and let everyone on the server rotate it but prevent them
   from adding or removing an item:

    /ilock -a *r

   or equivalently:

    /ilock -access *rotate


Unlocking a Frame
-----------------



Player Command Reference
------------------------


Staff Commands Reference
------------------------



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

