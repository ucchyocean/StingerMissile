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

import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * ロックオン方式のミサイル発射プラグイン
 * @author ucchy
 */
public class StingerMissile extends JavaPlugin implements Listener {

    private static final String NAME = "Stinger";
    private static final String DISPLAY_NAME =
            ChatColor.BLUE.toString() + ChatColor.BOLD.toString() + NAME;
    
    protected static final String MISSILE_META_NAME = "StingerMissileBullet";
    protected static final EntityType MISSILE_ENTITY = EntityType.ENDER_PEARL;
    protected static final Material MISSILE_MATERIAL = Material.ENDER_PEARL;
    
    private static final String EXPLOSION_SOURCE_META_NAME = "ExplosionSource";
    
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
        
        reloadLockonConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // タスクマネージャを作成
        targetingTasks = new HashMap<String, TargetingTask>();
        hormingTasks = new HashMap<String, ArrayList<HormingTask>>();

        // ミサイルランチャーを作成、レシピを設定
        item = makeLauncher();
        getServer().addRecipe(makeLauncherRecipe(item));

        // ColorTeaming のロード
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            Plugin colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( Utility.isUpperVersion(ctversion, "2.2.5") ) {
                getLogger().info("ColorTeaming was loaded. " 
                        + getDescription().getName() + " is in cooperation with ColorTeaming.");
                ColorTeamingBridge bridge = new ColorTeamingBridge(colorteaming);
                bridge.registerItem(item, NAME, DISPLAY_NAME);
            } else {
                getLogger().warning("ColorTeaming was too old. The cooperation feature will be disabled.");
                getLogger().warning("NOTE: Please use ColorTeaming v2.2.5 or later version.");
            }
        }
    }
    
    /**
     * 設定を再読み込みする
     */
    private void reloadLockonConfig() {
        
        // 必要に応じて、config.ymlを新規作成
        File configFile = new File(getDataFolder(), "config.yml");
        if ( !configFile.exists() ) {
            Utility.copyFileFromJar(getFile(), configFile, "config_ja.yml", false);
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
        
        item = new ItemStack(Material.GOLD_HOE, 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(wirerodMeta);
        return item;
    }
    
    /**
     * ランチャーのレシピを作成する
     */
    private Recipe makeLauncherRecipe(ItemStack launcher) {
        
        ShapelessRecipe recipe = new ShapelessRecipe(launcher);
        recipe.addIngredient(Material.GOLD_HOE);
        recipe.addIngredient(Material.ENDER_PEARL);
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
            BukkitTask t = 
                    Bukkit.getScheduler().runTaskTimer(
                            this, task, 0, config.getTargetingTicks());
            task.setId(t.getTaskId());
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
                
                Player attacker = Bukkit.getPlayerExact(
                        damagee.getMetadata(EXPLOSION_SOURCE_META_NAME).get(0).asString());
                damagee.removeMetadata(EXPLOSION_SOURCE_META_NAME, this);
                
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
            Player shooter = null;
            if ( proj.getShooter() instanceof Player ) {
                shooter = (Player)proj.getShooter();
            }
            if ( shooter != null ) {
                for ( Entity entity : proj.getNearbyEntities(power, power, power) ) {
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
                sender.sendMessage(ChatColor.RED + "このコマンドはゲーム内から実行してください。");
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
                sender.sendMessage(ChatColor.RED + "引数が足らないです。渡す相手を指定してください。");
                return true;
            }
            
            Player player = Bukkit.getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "指定されたプレイヤーが見つかりません。");
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
            
            reloadLockonConfig();
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
     * プレイヤーがテレポートしたときに呼び出されるメソッド
     * @param event 
     */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        
        if ( event.getCause() == TeleportCause.ENDER_PEARL ) {
            
            // テレポート無効カウントを取得する
            Player player = event.getPlayer();
            if ( player.hasMetadata(MISSILE_META_NAME) ) {
                MetadataValue mvalue = player.getMetadata(MISSILE_META_NAME).get(0);
                int count = mvalue.asInt();
                
                if ( count > 0 ) {
                    // カウントを1減らして、テレポートを無効化する
                    count--;
                    player.setMetadata(MISSILE_META_NAME, new FixedMetadataValue(this, count));
                    event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * 対象プレイヤーのテレポート無効カウントを1増やす
     * @param player 
     */
    protected void increasePlayerIgnoreTeleportCount(Player player) {
        
        int count = 1;
        if ( player.hasMetadata(MISSILE_META_NAME) ) {
            MetadataValue mvalue = player.getMetadata(MISSILE_META_NAME).get(0);
            count += mvalue.asInt();
        }
        player.setMetadata(MISSILE_META_NAME, new FixedMetadataValue(this, count));
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
        
        return DISPLAY_NAME.equals(item.getItemMeta().getDisplayName());
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
}
