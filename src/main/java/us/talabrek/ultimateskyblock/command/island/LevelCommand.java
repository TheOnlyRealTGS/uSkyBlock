package us.talabrek.ultimateskyblock.command.island;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.handler.VaultHandler;
import us.talabrek.ultimateskyblock.island.BlockScore;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandScore;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;
import java.util.logging.Level;

public class LevelCommand extends RequireIslandCommand {
    public LevelCommand(uSkyBlock plugin) {
        super(plugin, "level|info", "usb.island.info", "?island", "check your or anothers island level");
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (!Settings.island_useIslandLevel) {
            player.sendMessage("\u00a74Island level has been disabled, contact an administrator.");
            return true;
        }
        if (args.length == 0) {
            if (!plugin.playerIsOnIsland(player)) {
                player.sendMessage("\u00a7eYou must be on your island to use this command.");
                return true;
            }
            if (!plugin.onInfoCooldown(player) || Settings.general_cooldownInfo == 0) {
                plugin.setInfoCooldown(player);
                if (!island.isParty() && !pi.getHasIsland()) {
                    player.sendMessage("\u00a74You do not have an island!");
                } else {
                    getIslandLevel(player, player.getName(), alias);
                }
                return true;
            }
            player.sendMessage("\u00a7eYou can use that command again in " + plugin.getInfoCooldownTime(player) / 1000L + " seconds.");
            return true;
        }
        return false;
    }

    public boolean getIslandLevel(final Player player, final String islandPlayer, final String cmd) {
        PlayerInfo info = plugin.getPlayerInfo(islandPlayer);
        if (!info.getHasIsland() && !plugin.getIslandInfo(info).isParty()) {
            player.sendMessage("\u00a74That player is invalid or does not have an island!");
            return false;
        }
        final PlayerInfo playerInfo = islandPlayer.equals(player.getName()) ? plugin.getPlayerInfo(player) : new PlayerInfo(islandPlayer);
        if (player.getName().equals(playerInfo.getPlayerName())) {
            plugin.getIslandLogic().loadIslandChunks(playerInfo.getIslandLocation(), Settings.island_protectionRange / 2);
        }
        final IslandScore[] shared = new IslandScore[1];
        final Runnable showInfo = new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage("\u00a7eInformation about " + islandPlayer + "'s Island:");
                    if (cmd.equalsIgnoreCase("info") && shared[0] != null) {
                        player.sendMessage("Score Count Block");
                        for (BlockScore score : shared[0].getTop(10)) {
                            player.sendMessage(score.getState().getColor() + String.format("%05.2f  %d %s",
                                    score.getScore(), score.getCount(),
                                    VaultHandler.getItemName(score.getBlock())));
                        }
                        player.sendMessage(String.format(ChatColor.GREEN + "Island level is %5.2f", shared[0].getScore()));
                    } else {
                        player.sendMessage(String.format(ChatColor.GREEN + "Island level is %5.2f", plugin.getIslandInfo(playerInfo).getLevel()));
                    }
                }
            }
        };
        if (player.getName().equals(playerInfo.getPlayerName())) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    if (player.getName().equals(playerInfo.getPlayerName())) {
                        try {
                            IslandScore score = plugin.getLevelLogic().calculateScore(playerInfo);
                            plugin.getIslandInfo(playerInfo).setLevel(score.getScore());
                            playerInfo.save();
                            if (cmd.equalsIgnoreCase("info")) {
                                shared[0] = score;
                            }
                        } catch (Exception e) {
                            uSkyBlock.log(Level.SEVERE, "Error while calculating Island Level", e);
                        }
                    }
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, showInfo, 0L);
                }
            });
        } else {
            showInfo.run();
        }
        return true;
    }


}
