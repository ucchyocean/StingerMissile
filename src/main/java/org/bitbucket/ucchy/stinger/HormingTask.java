/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * ミサイルのホーミング処理タスク
 * @author ucchy
 */
public class HormingTask extends BukkitRunnable {

    private boolean isEnd;
    private Projectile missile;
    private Entity target;
    private int range;
    private int number;
    private int maxHorming;
    private double accelSpeed;
    private double maxSpeed;
    private double antiGravity;

    /**
     * コンストラクタ
     * @param missile ミサイルエンティティ
     * @param target ホーミング先
     * @param range 追尾可能な範囲設定
     * @param maxHorming ホーミング最大実行回数
     * @param accelSpeed 加速度
     * @param maxSpeed 最大速度
     * @param antiGravity 反重力量
     */
    public HormingTask(Projectile missile, Entity target, int range, int maxHorming,
            double accelSpeed, double maxSpeed, double antiGravity) {
        this.missile = missile;
        this.target = target;
        this.range = range;
        this.number = 0;
        this.maxHorming = maxHorming;
        this.isEnd = false;
        this.accelSpeed = accelSpeed;
        this.maxSpeed = maxSpeed;
        this.antiGravity = antiGravity;
    }

    /**
     * タスクが終了状態かどうか確認する
     * @return
     */
    protected boolean isEnded() {
        return isEnd;
    }

    /**
     * タイマーで呼び出されるメソッド
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        number++;

        if ( missile == null || missile.isDead() ) {
            // ミサイルが消滅しているようなのでタスクを終了する
            endThisTask();
            return;
        }

        if ( target == null || target.isDead() ) {
            // 対象を見失ったので追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            endThisTask();
            return;
        }

        if ( !target.getWorld().getName().equals(missile.getWorld().getName()) ) {
            // 対象のワールドとミサイルのワールドが異なる場合は、追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            endThisTask();
            return;
        }

        if ( number > maxHorming ) {
            // 最大ホーミング実行回数を超えたので、追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            endThisTask();
            return;
        }

        double distance = target.getLocation().distance(missile.getLocation());
        if ( distance > (double)range ) {
            // 距離が離れすぎたため追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            endThisTask();
            return;
        }

        // 追尾する
        Vector accel = target.getLocation().subtract(missile.getLocation()).toVector();
        accel.normalize().multiply(accelSpeed);

        Vector velocity = missile.getVelocity();
        velocity.add(accel);
        velocity.add(new Vector(0, antiGravity, 0));
        if ( velocity.length() > maxSpeed ) {
            velocity.normalize().multiply(maxSpeed);
        }
        missile.setVelocity(velocity);
    }

    /**
     * ミサイルのエンティティを削除する
     * @param missile
     */
    private void removeMissile(Projectile missile) {

        // エフェクト用の花火を削除する、ミサイルを削除する
        Entity passenger = missile.getPassenger();
        if ( passenger != null ) {
            passenger.remove();
        }
        missile.remove();
    }

    /**
     * このタスクを完了状態にして、繰り返しタスクを終了する
     */
    private void endThisTask() {
        cancel();
        isEnd = true;
    }
}
