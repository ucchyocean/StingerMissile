/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * ターゲッティング操作の処理タスク
 * @author ucchy
 */
public class TargetingTask extends BukkitRunnable {

    private boolean isEnd;
    private boolean isTargeting;
    private Player player;
    private ArrayList<LivingEntity> targeted;
    private int range;
    private double width;
    private int max;
    private boolean infiniteMissileMode;
    private Material consumeItem;

    /**
     * コンストラクタ
     * @param player
     */
    public TargetingTask(Player player) {
        this.player = player;
        this.isTargeting = true;
        this.targeted = new ArrayList<LivingEntity>();
        this.isEnd = false;

        range = StingerMissile.config.getTargetingRange();
        width = StingerMissile.config.getTargetingWidth();
        max = StingerMissile.config.getMaxTargetNum();
        consumeItem = StingerMissile.config.getConsumeMissileMaterial();
        infiniteMissileMode = StingerMissile.config.isInfiniteMissileMode();
    }

    /**
     * タスクが終了状態かどうか確認する
     * @return
     */
    protected boolean isEnded() {
        return isEnd;
    }

    /**
     * 次のタイマー処理でも、ターゲッティング処理を続行するように設定する。
     */
    protected void setTargeting() {
        this.isTargeting = true;
    }

    /**
     * タイマーで呼び出されるメソッド
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        if ( isTargeting ) {

            if ( targeted.size() < max ) {
                LivingEntity target = getTargetedEntity(player, range, width);
                if ( target != null && !targeted.contains(target) ) {

                    // ターゲット対象の名前を取得する
                    String name;
                    if ( target instanceof Player ) {
                        name = ((Player)target).getName();
                    } else {
                        name = target.getType().toString().toLowerCase();
                    }

                    // ターゲットしたことを通知して、音を鳴らす
                    String message = String.format(
                            ChatColor.GOLD + "targeted %s. (%d/%d)",
                            name, targeted.size() + 1, max);
                    player.sendMessage(message);
                    targeted.add(target);
                    player.playSound(
                            player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                }
            }

            isTargeting = false;

        } else {

            // ミサイルと、ホーミング処理タスクを作成する
            ArrayList<Projectile> missiles = new ArrayList<Projectile>();
            ArrayList<HormingTask> tasks = new ArrayList<HormingTask>();

            // ターゲット対象ごとに、ミサイルと、ホーミング処理タスクを生成する
            for ( LivingEntity target : targeted ) {

                // ターゲットした対象が既にいない場合は、ミサイルを発射しない
                if ( target == null || target.isDead() ) {
                    continue;
                }

                // 有限ミサイルモードで、
                // 手持ちにミサイルが無いなら発射しない、持っているなら1つ消費する
                if ( !infiniteMissileMode ) {
                    if ( !hasMissile(player) ) {
                        String materialName = consumeItem.name().toString();
                        player.sendMessage(ChatColor.RED
                                + "You don't have missile(" + materialName + ") !");
                        continue;
                    } else {
                        consumeMissile(player);
                    }
                }

                // ミサイルエンティティ生成
                double offset = player.getVelocity().length() * 2.0 + 1.0;
                Location location = player.getEyeLocation().add(
                        player.getLocation().getDirection().normalize().multiply(offset));
                Projectile missile = (Projectile)player.getWorld().spawnEntity(
                        location, StingerMissile.MISSILE_ENTITY);
                Vector vector = location.getDirection();
                vector.normalize().multiply(StingerMissile.config.getMissileAccelSpeed());
                missile.setVelocity(vector);
                missile.setMetadata(StingerMissile.MISSILE_META_NAME,
                      new FixedMetadataValue(StingerMissile.instance, true));
                missile.setShooter(player);

                // ミサイルのエフェクト生成
                Firework fw = (Firework)target.getWorld().spawnEntity(
                        target.getLocation(), EntityType.FIREWORK);
                FireworkMeta fwm = fw.getFireworkMeta();
                fwm.setPower(10);
                fw.setFireworkMeta(fwm);
                missile.setPassenger(fw);

                // ホーミング処理タスク生成
                int hormingRange = StingerMissile.config.getHormingRange();
                int hormingStart = StingerMissile.config.getHormingStartTicks();
                int hormingTicks = StingerMissile.config.getHormingTicks();
                int maxHorming = StingerMissile.config.getHormingNum();
                HormingTask task = new HormingTask(
                        missile, target, hormingRange, maxHorming);
                task.runTaskTimer(StingerMissile.instance, hormingStart, hormingTicks);

                missiles.add(missile);
                tasks.add(task);

                // テレポートを1回分無効にする。
                // （ミサイルにエンダーパールを使用するための回避策）
                StingerMissile.instance.increasePlayerIgnoreTeleportCount(player);
            }

            if ( missiles.size() == 0 ) {
                // 発射したミサイルが無い場合
                // プシュっという音と、煙を出す。

                player.playSound(player.getLocation(), Sound.IRONGOLEM_THROW, 1.0F, 1.5F);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 1);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 5);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 7);

            } else {
                // 発射したミサイルがある場合

                // 発射音を鳴らす
                player.getWorld().playSound(player.getLocation(), Sound.NOTE_BASS_DRUM, 1.0F, 1.0F);
                player.getWorld().playSound(player.getLocation(), Sound.SILVERFISH_HIT, 0.5F, 0.0F);
                player.getWorld().playSound(player.getLocation(), Sound.CHEST_CLOSE, 1.0F, 1.8F);

                // 発射タスクを記録する
                StingerMissile.instance.putHormingTask(player, tasks);

                // 発射したミサイルの数だけ、ランチャーの消耗度を追加する
                short durability = player.getItemInHand().getDurability();
                durability += (short)missiles.size();
                if ( durability >= player.getItemInHand().getType().getMaxDurability() ) {
                    // 消耗度が耐久度を超えたので、ランチャーを壊す。
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_BREAK, 1, 1);
                    player.setItemInHand(null);
                } else {
                    player.getItemInHand().setDurability(durability);
                }
            }

            // このタスクを終了する
            endThisTask();
        }
    }

    /**
     * ターゲット先にいるLivingEntityを取得する。
     * 最大100マス先のLivingEntityまで取得が可能。
     * ブロック（block.getType() != Material.AIR）に遮られている場合や、
     * 何も取得できなかった場合は、nullが返される。
     * @param player プレイヤー
     * @param range 取得距離
     * @param width 取得範囲
     */
    protected LivingEntity getTargetedEntity(Player player, int range, double width) {

        // プレイヤーと、レンジのちょうど中間にダミーEntityを生成し、
        // 立方体範囲のエンティティをまとめて取得する。
        StingerMissileConfig config = StingerMissile.config;
        Location center = player.getLocation().clone();
        double halfrange = (double)range / 2.0;
        center.add(center.getDirection().normalize().multiply(halfrange));
        Entity orb = center.getWorld().spawnEntity(center, EntityType.EXPERIENCE_ORB);
        ArrayList<LivingEntity> livings = new ArrayList<LivingEntity>();
        for ( Entity e : orb.getNearbyEntities(halfrange, halfrange, halfrange) ) {

            if ( e instanceof LivingEntity && !player.equals(e) ) {

                if ( e instanceof Player && config.isTargetingToPlayer() && !player.equals(e) ) {
                    // 対象がプレイヤーで、プレイヤーへのターゲッティングが有効なら、
                    // ターゲット対象に加える。
                    livings.add((LivingEntity)e);

                } else if ( !(e instanceof Player) && config.isTargetingToMob() ) {
                    // 対象がMOBで、MOBへのターゲッティングが有効なら、
                    // ターゲット対象に加える。
                    livings.add((LivingEntity)e);
                }
            }
        }
        orb.remove();

        // LivingEntity が1体も見つからないなら、そのまま終了する。
        if ( livings.isEmpty() ) {
            return null;
        }

        // 視線上のブロックを取得
        BlockIterator it = new BlockIterator(player, range);

        double widthSqr = width * width;

        // 手前側から検証を行う。
        // Blockか、LivingEntity が取得できた時点でreturnして終了する。
        while ( it.hasNext() ) {

            Block block = it.next();

            if ( block.getType() != Material.AIR ) {
                // ブロックが見つかったので終了する
                return null;

            } else {

                for ( LivingEntity le : livings ) {
                    if ( block.getLocation().distanceSquared(le.getLocation()) <= widthSqr ) {
                        // LivingEntityが見つかったので、LivingEntityを返して終了する
                        return le;
                    }
                }
            }
        }

        // 何も見つからなかった
        return null;
    }

    /**
     * 指定されたプレイヤーがミサイルを持っているかどうかを確認する
     * @param player
     * @return
     */
    private boolean hasMissile(Player player) {

        PlayerInventory inv = player.getInventory();
        int index = inv.first(consumeItem);
        return (index != -1);
    }

    /**
     * 指定されたプレイヤーが持つミサイルを1つ消費する
     * @param player
     */
    private void consumeMissile(Player player) {

        PlayerInventory inv = player.getInventory();
        int index = inv.first(consumeItem);
        ItemStack item = inv.getItem(index);
        int amount = item.getAmount() - 1;
        if ( amount <= 0 ) {
            inv.setItem(index, null);
        } else {
            item.setAmount(amount);
        }
    }

    /**
     * このタスクを完了状態にして、繰り返しタスクを終了する
     */
    private void endThisTask() {
        cancel();
        isEnd = true;
    }
}
