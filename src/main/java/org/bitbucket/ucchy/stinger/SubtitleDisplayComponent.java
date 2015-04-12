/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.stinger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * サブタイトル部分にメッセージを表示するためのコンポーネント
 * @author ucchy
 */
public class SubtitleDisplayComponent {

    public static void display(Player player, String text,
            int fadein, int duration, int fadeout) {

        if ( !Utility.isCB18orLater() ) {
            return;
        }

        CommandSender sender = Bukkit.getConsoleSender();

        String command = String.format("title %s times %d %d %d",
                player.getName(), fadein, duration, fadeout);
        Bukkit.dispatchCommand(sender, command);

        command = String.format("title %s subtitle {text:\"%s\"}",
                player.getName(), Utility.replaceColorCode(text));
        Bukkit.dispatchCommand(sender, command);

        command = String.format("title %s title {text:\"\"}", player.getName());
        Bukkit.dispatchCommand(sender, command);
    }
}
