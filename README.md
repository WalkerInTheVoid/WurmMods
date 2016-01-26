# WurmMods
Mods for Wurm Unlimited, by WalkerInTheVoid

Hi, these are mods I've made for Wurm Unlimited.  Below are the descriptions, and each bullet point is its own configuration in the .properties file of the mod in question.

You will need [ago's ModLauncher](https://github.com/ago1024/WurmServerModLauncher/releases) to use these mods.

Installation:

1. Place the mod's folder and .disabled file in the /mods directory of your wurm server's folder.
* Rename the .disabled filed to .properties
* (Re)Start the server.

Features (note that each bullet point is independently configurable): 

### BoatMod
 * Change the wind effect for all boats to be like a rowboat, with a commensurate increase in speed, up to the cap.
 * Make all animals able to swim while led by a player
 * Enable continued leading of animals when embarking on a vehicle (no more having to frantically re-lead all your critters when you hop in the boat)


### BountyMod
 * Enable skillgain for fighting/killing player-bred creatures
 * Convert the chance of receiving a coin award from a slim chance of a rare coin to a certainty of a non-rare coin.  Player-bred creatures offer lesser bounties.  (Note that skillgain for player-bred creatures has to be enabled to award bounties for such kills)
 * Bounty for each species of critter can be independently configured
 * Bounty can be paid to player's bank account, instead of to inventory as coins
 * Multiplier for all bounties in general, for ease of fine-tuning for your server
 * Multiplier for player-bred creatures is configurable (default is 0.1, or one-tenth)
 * Bounties can be paid out of thin air (thus unlimited bounty payments) or from the kingdom coffers (thanks Netscreever)
 * Non-hostile, same-kingdom creatures can be set to pay out no bounty (thanks again Netscreever)

### BulkMod
 * Enables placing hot items into BSBs and FSBs, no need to wait for them to cool down.

A bit bare bones but I'll probably come back to this one.

### DigLikeMining
* A recreation of [Alexgopen's Digging Like Mining](http://forum.wurmonline.com/index.php?/topic/132826-wip-digging-like-mining/) for ago's modloader.  While I didn't have Alexgopen's original code, his work did tell me which classes I needed to modify.

In addition, added dredging to ship, and using dirt/sand piles on the ground you're standing on when flattening or leveling tiles (any dirt in your inventory will be used first, followed by dirt on the ground, followed by sand in your inventory, and finally sand on the ground).
 
### MeditateMod
  * Enables guaranteed skill success (no more "You fail to relax" or not getting a path question you qualify for)
  * No delay between first 5 skill gains of the day
  * No difference in delay requirements after the first 5 meditations (if the above setting is on, there's no delay requirements for meditation skill gain at all, if it's off, always 30 minutes between meditation skill gains, never 3 hours)
  * Disable distance requirements for meditation skill gains (No more need to move 11 tiles for skillgain)
  * A configurable multiplier for path question cooldowns.  This can be set to half to cut these delays in half, set to 0 to disable them entirely, the choice is yours.
  * Specify custom action duration for meditation
  
### PhobiaMod
Don't like the spider models?  Swap out spiders with other mobs!
  * Replace Huge Spiders with Black Bears
  * Replace Lava Spiders with Lava Fiends

### ProspectMod
  * Prospecting cave floors provides the same information as prospecting a rock tile on the surface above (i.e. detect nearby ore veins).

### SacrificeMod
  * Adds a configurable chance for a rare item to award a bone of the same rarity, instead of the usual benefits.

### SalveMod
  * Adds "power - " to the beginning of newly made healing cover names, so you no longer have to figure it out in your head.


