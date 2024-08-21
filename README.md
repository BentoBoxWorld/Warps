# Warps
Add-on for BentoBox to add personal warp signs for players of BSkyBlock or AcidIsland.
This add-on will work for all game modes installed on a BentoBox server. Use config settings
to disable use by gamemode.

![warps](https://github.com/user-attachments/assets/3454b7ec-d9bf-4631-9e5a-d6e603ac5f15)



## How to use

### Note: Java 16 and Minecraft 17, or later are required.

1. Place the jar in the addons folder of the BentoBox plugin
2. Restart the server
3. The addon will create a data folder and inside the folder will be a config.yml
4. Edit the config.yml how you want
5. Restart the server if you make a change

## Config.yml

These are options in the config:

### warplevelrestriction

This is the minimum island level needed to be able to create a warp.
0 or negative values will disable this restriction and 10 is default.

If you do not have the level addon installed, this will have no effect.

### Warp Sign Text - welcomeLine

This is the text a player must put on the first line of the sign to make it a warp sign.

### Icon

Icon that will be displayed in Warps list. SIGN counts for any kind of sign and the type of
wood used will be reflected in the panel if the server supports it.

It uses native Minecraft material strings, but using string 'PLAYER_HEAD', it is possible to
use player heads instead. Beware that Mojang API rate limiting may prevent heads from loading.

Default is 'SIGN'.

### Disabled Game Modes

This list stores GameModes in which Level addon should not work.
To disable addon it is necessary to write its name in new line that starts with -. Example:

```
  disabled-gamemodes:
  - BSkyBlock
```
Default is that all game modes can use Warps
