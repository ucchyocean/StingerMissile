/*
 * @author     ucchy
 * @license    All Rights Reserved
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * ユーティリティクラス
 * @author ucchy
 */
public class Utility {

    /**
     * jarファイルの中に格納されているファイルを、jarファイルの外にコピーするメソッド
     * @param jarFile jarファイル
     * @param targetFile コピー先
     * @param sourceFilePath コピー元
     * @param isBinary バイナリファイルかどうか
     */
    public static void copyFileFromJar(
            File jarFile, File targetFile, String sourceFilePath, boolean isBinary) {

        InputStream is = null;
        FileOutputStream fos = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        File parent = targetFile.getParentFile();
        if ( !parent.exists() ) {
            parent.mkdirs();
        }

        try {
            JarFile jar = new JarFile(jarFile);
            ZipEntry zipEntry = jar.getEntry(sourceFilePath);
            is = jar.getInputStream(zipEntry);

            fos = new FileOutputStream(targetFile);

            if ( isBinary ) {
                byte[] buf = new byte[8192];
                int len;
                while ( (len = is.read(buf)) != -1 ) {
                    fos.write(buf, 0, len);
                }

            } else {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                // CB190以降は、書き出すファイルエンコードにUTF-8を強制する。
                if ( isCB19orLater() ) {
                    writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
                } else {
                    writer = new BufferedWriter(new OutputStreamWriter(fos));
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( writer != null ) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( fos != null ) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( is != null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
    }

    /**
     * 同じチームに所属していて、そのチームがFF無効かどうかを確認する
     * @param player1
     * @param player2
     * @return 同じチームでFF無効かどうか
     */
    @SuppressWarnings("deprecation")
    public static boolean checkPlayersSameTeam(Player player1, Player player2) {

        Scoreboard board = player1.getScoreboard();
        if ( board == null ) {
            return false;
        }

        Team team1 = null;
        Team team2 = null;
        if ( isCB186orLater() ) {
            team1 = board.getEntryTeam(player1.getName());
            team2 = board.getEntryTeam(player2.getName());
        } else {
            team1 = board.getPlayerTeam(player1);
            team2 = board.getPlayerTeam(player2);

        }

        if ( team1 == null || team2 == null ) {
            return false;
        }

        return team1.getName().equals(team2.getName()) && !team1.allowFriendlyFire();
    }


    private static Boolean isCB18orLaterCache;
    private static Boolean isCB186orLaterCache;
    private static Boolean isCB19orLaterCache;

    /**
     * 現在動作中のCraftBukkitが、v1.8 以上かどうかを確認する
     * @return v1.8以上ならtrue、そうでないならfalse
     */
    public static boolean isCB18orLater() {
        if ( isCB18orLaterCache == null ) {
            isCB18orLaterCache = isUpperVersion(Bukkit.getBukkitVersion(), "1.8");
        }
        return isCB18orLaterCache;
    }

    /**
     * 現在動作中のCraftBukkitが、v1.8.6 以上かどうかを確認する
     * @return v1.8.6以上ならtrue、そうでないならfalse
     */
    public static boolean isCB186orLater() {
        if ( isCB186orLaterCache == null ) {
            isCB186orLaterCache = isUpperVersion(Bukkit.getBukkitVersion(), "1.8.6");
        }
        return isCB186orLaterCache;
    }

    /**
     * 現在動作中のCraftBukkitが、v1.9 以上かどうかを確認する
     * @return v1.9以上ならtrue、そうでないならfalse
     */
    public static boolean isCB19orLater() {
        if ( isCB19orLaterCache == null ) {
            isCB19orLaterCache = isUpperVersion(Bukkit.getBukkitVersion(), "1.9");
        }
        return isCB19orLaterCache;
    }

    /**
     * 指定されたバージョンが、基準より新しいバージョンかどうかを確認する
     * @param version 確認するバージョン
     * @param border 基準のバージョン
     * @return 基準より確認対象の方が新しいバージョンかどうか<br/>
     * ただし、無効なバージョン番号（数値でないなど）が指定された場合はfalseに、
     * 2つのバージョンが完全一致した場合はtrueになる。
     */
    private static boolean isUpperVersion(String version, String border) {

        int hyphen = version.indexOf("-");
        if ( hyphen > 0 ) {
            version = version.substring(0, hyphen);
        }

        String[] versionArray = version.split("\\.");
        int[] versionNumbers = new int[versionArray.length];
        for ( int i=0; i<versionArray.length; i++ ) {
            if ( !versionArray[i].matches("[0-9]+") )
                return false;
            versionNumbers[i] = Integer.parseInt(versionArray[i]);
        }

        String[] borderArray = border.split("\\.");
        int[] borderNumbers = new int[borderArray.length];
        for ( int i=0; i<borderArray.length; i++ ) {
            if ( !borderArray[i].matches("[0-9]+") )
                return false;
            borderNumbers[i] = Integer.parseInt(borderArray[i]);
        }

        int index = 0;
        while ( (versionNumbers.length > index) && (borderNumbers.length > index) ) {
            if ( versionNumbers[index] > borderNumbers[index] ) {
                return true;
            } else if ( versionNumbers[index] < borderNumbers[index] ) {
                return false;
            }
            index++;
        }
        if ( borderNumbers.length == index ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * カラーコードの置き換えを行う
     * @param source
     * @return 置き換えられた文字列
     */
    public static String replaceColorCode(String source) {
        return ChatColor.translateAlternateColorCodes('&', source);
    }

    /**
     * 指定された名前のプレイヤーを返す
     * @param name プレイヤー名
     * @return プレイヤー、該当プレイヤーがオンラインでない場合はnullになる。
     */
    @SuppressWarnings("deprecation")
    public static Player getPlayerExact(String name) {
        return Bukkit.getPlayerExact(name);
    }

    /**
     * プレイヤーが手（メインハンド）に持っているアイテムを取得します。
     * @param player プレイヤー
     * @return 手に持っているアイテム
     */
    @SuppressWarnings("deprecation")
    public static ItemStack getItemInHand(Player player) {
        if ( Utility.isCB19orLater() ) {
            return player.getInventory().getItemInMainHand();
        } else {
            return player.getItemInHand();
        }
    }

    /**
     * 指定したプレイヤーの手に持っているアイテムを設定します。
     * @param player プレイヤー
     * @param item アイテム
     */
    @SuppressWarnings("deprecation")
    public static void setItemInHand(Player player, ItemStack item) {
        if ( Utility.isCB19orLater() ) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }
    }
}
