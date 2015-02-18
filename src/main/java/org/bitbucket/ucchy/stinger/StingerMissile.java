/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ロックオン方式のミサイル発射プラグイン
 * @author ucchy
 */
public class StingerMissile extends JavaPlugin implements Listener {

    protected static final String MISSILE_META_NAME = "StingerMissile";
    protected static final EntityType MISSILE_ENTITY = EntityType.SNOWBALL;
    protected static final Material MISSILE_MATERIAL = Material.ENDER_PEARL;

    private static final String EXPLOSION_SOURCE_META_NAME = "ExplosionSource";

    private static final String CONFIG_FILE_NAME_EN = "config.yml";
    private static final String CONFIG_FILE_NAME_JA = "config_ja.yml";

    private ItemStack item;

    protected static StingerMissile instance;
    protected static StingerMissileConfig config;

    private HashMap<String, TargetingTask> targetingTasks;
    private HashMap<String, ArrayList<HormingTask>> hormingTasks;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable() {

        instance = this;

        reloadStingerConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // タスクマネージャを作成
        targetingTasks = new HashMap<String, TargetingTask>();
        hormingTasks = new HashMap<String, ArrayList<HormingTask>>();

        // ミサイルランチャーを作成、レシピを設定
        item = makeLauncher();
        getServer().addRecipe(makeLauncherRecipe(item));
    }

    /**
     * 設定を再読み込みする
     */
    private void reloadStingerConfig() {

        // 必要に応じて、config.ymlを新規作成
        File configFile = new File(getDataFolder(), "config.yml");
        if ( !configFile.exists() ) {
            String configFileOrg = CONFIG_FILE_NAME_EN;
            if ( getDefaultLocaleLanguage().equals("ja") ) {
                configFileOrg = CONFIG_FILE_NAME_JA;
            }
            Utility.copyFileFromJar(getFile(), configFile, configFileOrg, false);
        }

        // config.yml の再読み込み
        reloadConfig();
        config = new StingerMissileConfig(getConfig());
    }

    /**
     * ランチャーを作成して返す
     * @return ランチャー
     */
    private ItemStack makeLauncher() {

        item = new ItemStack(config.getLauncherMaterial(), 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(config.getLauncherDisplayName());
        item.setItemMeta(wirerodMeta);
        return item;
    }

    /**
     * ランチャーのレシピを作成する
     */
    private Recipe makeLauncherRecipe(ItemStack launcher) {

        ShapelessRecipe recipe = new ShapelessRecipe(launcher);
        recipe.addIngredient(config.getLauncherMaterial());
        recipe.addIngredient(MISSILE_MATERIAL);
        return recipe;
    }

    /**
     * プレイヤーがクリックをしたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // ロックオン銃でなければ、無視する
        if ( !isLockonGun(item) ) {
            return;
        }

        // 感圧板イベントなら無視する。
        if ( event.getAction() == Action.PHYSICAL ) {
            return;
        }

        // 右クリックによるイベントでなければ無視する。
        if ( event.getAction() == Action.LEFT_CLICK_AIR ||
                event.getAction() == Action.LEFT_CLICK_BLOCK ) {
            return;
        }

        // イベントのバニラ動作をキャンセル設定。
        event.setCancelled(true);

        // パーミッションを持っていないならメッセージを出して終了。
        if ( !checkPermission(player, "action") ) {
            return;
        }

        // このプレイヤーの弾は発射中かどうかを確認する。
        if ( checkHormingTask(player) ) {
            return; // 発射中なので終了
        }

        // ターゲッティング処理を行う
        TargetingTask task = null;
        if ( targetingTasks.containsKey(player.getName()) ) {
            task = targetingTasks.get(player.getName());
            if ( task.isEnded() ) {
                targetingTasks.remove(player.getName());
                task = null;
            }
        }
        if ( task == null ) {
            task = new TargetingTask(player);
            targetingTasks.put(player.getName(), task);
            task.runTaskTimer(this, 0, config.getTargetingTicks());
        } else {
            task.setTargeting();
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {

        // Damagerがミサイルで、メタデータを持っているなら、ダメージイベントをキャンセルする。
        // これをしないと、ミサイルが直接ターゲットにあたったときに、爆発ダメージが発生しない。
        Entity damager = event.getDamager();
        if ( damager != null && damager.getType() == MISSILE_ENTITY &&
                damager.hasMetadata(MISSILE_META_NAME) ) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        // EXPLOSION_SOURCE のメタデータを持っているなら、
        // 同じチームメンバーの攻撃かどうかを確認し、
        // 同じチームメンバーなら、ダメージをキャンセルする
        if ( event.getEntity() instanceof Player ) {

            Player damagee = (Player)event.getEntity();

            if ( event.getCause() == DamageCause.BLOCK_EXPLOSION &&
                    damagee.hasMetadata(EXPLOSION_SOURCE_META_NAME) ) {

                Player attacker = Utility.getPlayerExact(
                        damagee.getMetadata(EXPLOSION_SOURCE_META_NAME).get(0).asString());
                damagee.removeMetadata(EXPLOSION_SOURCE_META_NAME, this);

                // 追加ダメージを増やす
                event.setDamage(event.getDamage() + config.getExplosionDamage());

                if ( attacker != null ) {

                    if ( Utility.checkPlayersSameTeam(attacker, damagee) ) {
                        // 同じチームメンバーが起こした爆発なら、イベントをキャンセル
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        // ミサイルがぶつかったときに、爆発を発生させる

        Projectile proj = event.getEntity();
        if ( proj != null && proj.getType() == MISSILE_ENTITY &&
                proj.hasMetadata(MISSILE_META_NAME) ) {

            // まず、着弾地点の爆発範囲にいるエンティティに、
            // メタデータで攻撃者を記録し、ダメージ0を与えて攻撃者を記録する
            double power = config.getExplosionPower();
            double range = power * 2;
            Player shooter = null;
            if ( proj.getShooter() instanceof Player ) {
                shooter = (Player)proj.getShooter();
            }
            if ( shooter != null ) {
                for ( Entity entity : proj.getNearbyEntities(range, range, range) ) {
                    if ( entity instanceof LivingEntity ) {
                        LivingEntity le = (LivingEntity)entity;
                        le.setMetadata(EXPLOSION_SOURCE_META_NAME,
                                new FixedMetadataValue(this, shooter.getName()));
                        le.damage(0, shooter);
                        le.setNoDamageTicks(0);
                    }
                }
            }

            // 爆発を発生させてダメージを与える
            Location l = proj.getLocation();
            World w = proj.getWorld();
            w.createExplosion(
                    l.getX(), l.getY(), l.getZ(),
                    (float)power, false, false);
            proj.removeMetadata(MISSILE_META_NAME, this);

            // エフェクト用の花火を削除する、ミサイルを削除する
            Entity passenger = proj.getPassenger();
            if ( passenger != null ) {
                passenger.remove();
            }
            proj.remove();
        }
    }

    /**
     * コマンドが実行されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(
            CommandSender sender, Command command, String label, String[] args) {

        if ( args.length == 0 ) {
            return false;
        }

        if ( args[0].equalsIgnoreCase("get") ) {

            if ( !checkPermission(sender, "get") ) {
                return true;
            }

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command cannot be run from console.");
                return true;
            }

            Player player = (Player)sender;

            ItemStack temp = player.getItemInHand();
            player.setItemInHand(item.clone());
            if ( temp != null ) {
                player.getInventory().addItem(temp);
            }

            return true;

        } else if ( args[0].equalsIgnoreCase("give") ) {

            if ( !checkPermission(sender, "give") ) {
                return true;
            }

            if ( args.length <= 1 ) {
                sender.sendMessage(ChatColor.RED + "Please add player name. ex) /" + label + " give (player)");
                return true;
            }

            Player player = Utility.getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Not found the specified player.");
                return true;
            }

            ItemStack temp = player.getItemInHand();
            player.setItemInHand(item.clone());
            if ( temp != null ) {
                player.getInventory().addItem(temp);
            }

            return true;

        } else if ( args[0].equalsIgnoreCase("reload") ) {

            if ( !checkPermission(sender, "reload") ) {
                return true;
            }

            reloadStingerConfig();
            sender.sendMessage(ChatColor.GOLD + "reload completed.");
            return true;
        }

        return false;
    }

    /**
     * タブキー補完を実行したときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {

        if ( args.length == 1 ) {
            String pre = args[0];
            ArrayList<String> candidates = new ArrayList<String>();
            for ( String com : new String[]{"get", "give", "reload"} ) {
                if ( com.startsWith(pre) && sender.hasPermission("stinger." + com) ) {
                    candidates.add(com);
                }
            }
            return candidates;
        }

        return null;
    }

    /**
     * アイテムが、ロックオン銃であるかどうかを確認する
     * @param item
     * @return
     */
    private boolean isLockonGun(ItemStack item) {

        if ( item == null ) {
            return false;
        }

        if ( !item.hasItemMeta() ) {
            return false;
        }

        if ( !item.getItemMeta().hasDisplayName() ) {
            return false;
        }

        String displayName = config.getLauncherDisplayName();
        return displayName.equals(item.getItemMeta().getDisplayName());
    }

    /**
     * 指定したプレイヤーに関連する、ミサイル追跡タスクを設定する。
     * @param player
     * @param tasks
     */
    protected void putHormingTask(Player player, ArrayList<HormingTask> tasks) {
        hormingTasks.put(player.getName(), tasks);
    }

    /**
     * 指定したプレイヤーに関連した、ミサイル追跡タスクがあるかどうかを確認する
     * @param owner
     * @return
     */
    private boolean checkHormingTask(Player owner) {

        if ( !hormingTasks.containsKey(owner.getName()) ) {
            return false;
        }

        ArrayList<HormingTask> tasks = hormingTasks.get(owner.getName());
        for ( HormingTask task : tasks ) {
            if ( !task.isEnded() ) {
                return true;
            }
        }

        // 全てのタスクは終了しているので、クリーンアップする
        tasks.clear();
        hormingTasks.remove(owner.getName());

        return false;
    }

    /**
     * コマンドの実行権限を調べる
     * @param sender
     * @param name
     * @return 実行権限を持っているかどうか
     */
    private boolean checkPermission(CommandSender sender, String name) {
        boolean perm = sender.hasPermission("stinger." + name);
        if ( !perm ) {
            sender.sendMessage(ChatColor.RED
                    + "You don't have permission \"stinger." + name + "\".");
        }
        return perm;
    }

    /**
     * 動作環境の言語設定を取得する。日本語環境なら ja、英語環境なら en が返される。
     * @return 動作環境の言語
     */
    public static String getDefaultLocaleLanguage() {
        Locale locale = Locale.getDefault();
        if ( locale == null ) return "en";
        return locale.getLanguage();
    }
}
