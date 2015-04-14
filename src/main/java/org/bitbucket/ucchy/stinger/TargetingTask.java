/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import java.util.ArrayList;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
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
    private ArrayList<Entity> targeted;
    private Entity delayCandidate;
    private int remainDelayCycles;

    private int range;
    private double width;
    private int max;
    private Material consumeItem;
    private boolean infiniteMissileMode;
    private int lockonDelayCycle;
    private double missileAccelSpeed;
    private double missileMaxSpeed;
    private double missileAgainstGravity;

    private int hormingRange;
    private int hormingStart;
    private int hormingTicks;
    private int maxHorming;

    private StingerMissileConfig config;

    /**
     * コンストラクタ
     * @param player
     */
    public TargetingTask(Player player, StingerMissileConfig config) {
        this.player = player;
        this.isTargeting = true;
        this.targeted = new ArrayList<Entity>();
        this.isEnd = false;
        this.config = config;

        range = config.getTargetingRange();
        width = config.getTargetingWidth();
        max = config.getMaxTargetNum();
        consumeItem = config.getConsumeMissileMaterial();
        infiniteMissileMode = config.isInfiniteMissileMode();
        lockonDelayCycle = config.getLockonDelayCycle();
        missileAccelSpeed = config.getMissileAccelSpeed();
        missileMaxSpeed = config.getMissileMaxSpeed();
        missileAgainstGravity = config.getAgainstGravity();

        hormingRange = config.getHormingRange();
        hormingStart = config.getHormingStartTicks();
        hormingTicks = config.getHormingTicks();
        maxHorming = config.getHormingNum();
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
                Entity target = getTargetedEntity(player, range, width);
                if ( target != null && !targeted.contains(target) ) {

                    // ターゲット対象の名前を取得する
                    String name;
                    if ( target instanceof Player ) {
                        name = ((Player)target).getName();
                    } else {
                        name = target.getType().toString().toLowerCase();
                    }

                    // 遅延サイクルが必要なら、遅延サイクルを実行する
                    if ( lockonDelayCycle > 0 ) {
                        if ( delayCandidate == null || !delayCandidate.equals(target) ) {
                            remainDelayCycles = lockonDelayCycle - 1;
                            delayCandidate = target;
                            int distance =
                                    (int)target.getLocation().distance(player.getLocation());
                            String message =
                                    config.getMessageTargetCandidate(
                                    name, distance);
                            player.sendMessage(message);
                            config.getSoundLockonDelay().playSoundToPlayer(player);
                            return;
                        } else {
                            if ( remainDelayCycles > 0 ) {
                                remainDelayCycles--;
                                return;
                            }
                        }
                    }

                    // ターゲットしたことを通知して、音を鳴らす
                    String message = config.getMessageTargetted(
                            name, targeted.size() + 1, max);
                    player.sendMessage(message);
                    targeted.add(target);
                    config.getSoundLockonTarget().playSoundToPlayer(player);

                    // ターゲットされたのがプレイヤーなら、相手に警告音を鳴らす
                    Player targettedPlayer = null;
                    if ( target instanceof Player ) {
                        targettedPlayer = (Player)target;
                    } else if ( !target.isEmpty() && (target.getPassenger() instanceof Player) ) {
                        targettedPlayer = (Player)target.getPassenger();
                    }
                    if ( targettedPlayer != null ) {
                        config.getSoundWarning().playSoundToPlayer(targettedPlayer);
                        SubtitleDisplayComponent.display(targettedPlayer,
                                config.getMessageWarning(), 0, 15, 10);
                    }
                }
            }

            isTargeting = false;

        } else {

            // ミサイルと、ホーミング処理タスクを作成する
            ArrayList<Projectile> missiles = new ArrayList<Projectile>();
            ArrayList<HormingTask> tasks = new ArrayList<HormingTask>();

            // ターゲット対象ごとに、ミサイルと、ホーミング処理タスクを生成する
            for ( Entity target : targeted ) {

                // ターゲットした対象が既にいない場合は、ミサイルを発射しない
                if ( target == null || target.isDead() ) {
                    continue;
                }

                // 有限ミサイルモードで、
                // 手持ちにミサイルが無いなら発射しない、持っているなら1つ消費する
                if ( !infiniteMissileMode ) {
                    if ( !hasMissile(player) ) {
                        String materialName = consumeItem.name().toString();
                        player.sendMessage(config.getMessageEmptyMissile(materialName));
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

                if ( StingerMissile.MISSILE_ENTITY == EntityType.ARROW ) {
                    Arrow arrow = (Arrow)missile;
                    arrow.setCritical(true);
                }

                Vector vector = location.getDirection();
                vector.setY(vector.getY() + 0.5);
                vector.normalize().multiply(missileAccelSpeed);
                missile.setVelocity(vector);
                missile.setMetadata(StingerMissile.MISSILE_META_NAME,
                      new FixedMetadataValue(StingerMissile.getInstance(),
                              config.getLauncherDisplayName()));
                missile.setShooter(player);

                // ミサイルのエフェクト生成
                Firework fw = (Firework)target.getWorld().spawnEntity(
                        target.getLocation(), EntityType.FIREWORK);
                FireworkMeta fwm = fw.getFireworkMeta();
                fwm.setPower(10);
                fw.setFireworkMeta(fwm);
                missile.setPassenger(fw);

                // ホーミング処理タスク生成
                HormingTask task = new HormingTask(missile, target, hormingRange, maxHorming,
                        missileAccelSpeed, missileMaxSpeed, missileAgainstGravity);
                task.runTaskTimer(StingerMissile.getInstance(), hormingStart, hormingTicks);

                missiles.add(missile);
                tasks.add(task);

                // ターゲットされたのがプレイヤーなら、相手に警告音を鳴らす
                Player targettedPlayer = null;
                if ( target instanceof Player ) {
                    targettedPlayer = (Player)target;
                } else if ( !target.isEmpty() && (target.getPassenger() instanceof Player) ) {
                    targettedPlayer = (Player)target.getPassenger();
                }
                if ( targettedPlayer != null ) {
                    config.getSoundWarningMissileInbound().playSoundToPlayer(targettedPlayer);
                    SubtitleDisplayComponent.display(targettedPlayer,
                            config.getMessageWarningMissileInbound(), 0, 15, 10);
                }
            }

            if ( missiles.size() == 0 ) {
                // 発射したミサイルが無い場合
                // プシュっという音と、煙を出す。

                config.getSoundNoneTarget().playSoundToWorld(player.getLocation());
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 1);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 5);
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 7);

            } else {
                // 発射したミサイルがある場合

                // 発射音を鳴らす
                config.getSoundLaunching().playSoundToWorld(player.getLocation());

                // 発射タスクを記録する
                StingerMissile.getInstance().putHormingTask(player, tasks);

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
    protected Entity getTargetedEntity(Player player, int range, double width) {

        // プレイヤーと、レンジのちょうど中間にダミーEntityを生成し、
        // 立方体範囲のエンティティをまとめて取得する。
        Location center = player.getLocation().clone();
        double halfrange = (double)range / 2.0;
        center.add(center.getDirection().normalize().multiply(halfrange));
        Entity orb = center.getWorld().spawnEntity(center, EntityType.EXPERIENCE_ORB);
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for ( Entity e : orb.getNearbyEntities(halfrange, halfrange, halfrange) ) {

            if ( !player.equals(e) ) {

                if ( config.isTargetingToPlayer() && e instanceof Player ) {
                    // 対象がプレイヤーで、プレイヤーへのターゲッティングが有効なら、
                    // ターゲット対象に加える。
                    entities.add(e);

                } else if ( config.isTargetingToMob()
                        && !(e instanceof Player) && (e instanceof LivingEntity) ) {
                    // 対象がMOBで、MOBへのターゲッティングが有効なら、
                    // ターゲット対象に加える。
                    entities.add(e);

                } else if ( config.isTargetingToVehicle()
                        && (e instanceof Boat || e instanceof Minecart) ) {
                    // 対象がMinecartかBoatで、Vehicleへのターゲッティングが有効なら、
                    // ターゲット対象に加える。
                    entities.add(e);

                } else if ( config.isTargetingToEnderCrystal()
                        && e instanceof EnderCrystal) {
                    // 対象がEnderCrystalで、EnderCrystalへのターゲッティングが有効なら、
                    // ターゲット対象に加える。
                    entities.add(e);

                }
            }
        }
        orb.remove();

        // LivingEntity が1体も見つからないなら、そのまま終了する。
        if ( entities.isEmpty() ) {
            return null;
        }

        // 視線上のブロックを取得
        BlockIterator it = new BlockIterator(player, range);

        double widthSqr = width * width;

        // 手前側から検証を行う。
        // Blockか、Entity が取得できた時点でreturnして終了する。
        while ( it.hasNext() ) {

            Block block = it.next();

            if ( block.getType() != Material.AIR ) {
                // ブロックが見つかったので終了する
                return null;

            } else {

                for ( Entity e : entities ) {
                    if ( block.getLocation().distanceSquared(e.getLocation()) <= widthSqr ) {
                        // Entityが見つかったので、Entityを返して終了する
                        return e;
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
