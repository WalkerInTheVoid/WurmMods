# WurmMods
Mods for Wurm Unlimited, by WalkerInTheVoid

Hi, these are mods I've made for Wurm Unlimited.  Below are the descriptions, and each bullet point is its own configuration in the .properties file of the mod in question.

You will need [ago's ModLauncher](https://github.com/ago1024/WurmServerModLauncher/releases) to use these mods.

Installation:

1. Place the mod's folder and .disabled file in the /mods directory of your wurm server's folder.
* Rename the .disabled filed to .properties
* (Re)Start the server.

### BountyMod
 * Enable skillgain for fighting/killing player-bred creatures
 * Convert the chance of receiving a coin award from a slim chance of a rare coin to a certainty of a non-rare coin.  Player-bred creatures offer lesser bounties.  (Note that skillgain for player-bred creatures has to be enabled to award bounties for such kills)
 * Bounty for each species of critter can be independently configured

### BulkMod
 * Enables placing hot items into BSBs and FSBs, no need to wait for them to cool down.

A bit bare bones but I'll probably come back to this one.

### DigLikeMining
* A recreation of [Alexgopen's Digging Like Mining](http://forum.wurmonline.com/index.php?/topic/132826-wip-digging-like-mining/) for ago's modloader.  While I didn't have Alexgopen's original code, his work did tell me which classes I needed to modify.
 
### MeditateMod
  * Enables guaranteed skill success (no more "You fail to relax" or not getting a path question you qualify for)
  * No delay between first 5 skill gains of the day
  * No difference in delay requirements after the first 5 meditations (if the above setting is on, there's no delay requirements for meditation skill gain at all, if it's off, always 30 minutes between meditation skill gains, never 3 hours)
  * Disable distance requirements for meditation skill gains (No more need to move 11 tiles for skillgain)
  * **NEW**: A configurable multiplier for path question cooldowns.  This can be set to half to cut these delays in half, set to 0 to disable them entirely, the choice is yours.
  
### PhobiaMod
Don't like the spider models?  Swap out spiders with other mobs!
  * Replace Huge Spiders with Black Bears
  * Replace Lava Spiders with Lava Fiends

### ProspectMod
  * Prospecting cave floors provides the same information as prospecting a rock tile on the surface above (i.e. detect nearby ore veins).


