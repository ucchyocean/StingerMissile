/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.stinger;

import org.bukkit.Bukkit;
import org.bukkit.World;
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

        // sendCommandFeedbackの状態を取得、有効だったなら一旦無効にする。
        World world = player.getWorld();
        boolean pre = Boolean.parseBoolean(
                world.getGameRuleValue("sendCommandFeedback"));
        if ( pre ) world.setGameRuleValue("sendCommandFeedback", "false");

        // titleコマンドを実行
        CommandSender sender = Bukkit.getConsoleSender();
        String command = String.format("title %s times %d %d %d",
                player.getName(), fadein, duration, fadeout);
        Bukkit.dispatchCommand(sender, command);
        command = String.format("title %s subtitle {text:\"%s\"}",
                player.getName(), Utility.replaceColorCode(text));
        Bukkit.dispatchCommand(sender, command);
        command = String.format("title %s title {text:\"\"}", player.getName());
        Bukkit.dispatchCommand(sender, command);

        // sendCommandFeedbackの状態を戻す。
        if ( pre ) world.setGameRuleValue("sendCommandFeedback", "true");
    }
}
