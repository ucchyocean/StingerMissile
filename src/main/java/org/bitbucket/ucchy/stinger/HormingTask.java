/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * ミサイルのホーミング処理タスク
 * @author ucchy
 */
public class HormingTask extends BukkitRunnable {

    private int id;
    private boolean isEnd;
    private Projectile missile;
    private LivingEntity target;
    private int range;
    private int number;
    private int maxHorming;
    
    /**
     * コンストラクタ
     * @param missile ミサイルエンティティ
     * @param target ホーミング先
     * @param range 追尾可能な範囲設定
     * @param maxHorming ホーミング最大実行回数
     */
    public HormingTask(Projectile missile, LivingEntity target, int range, int maxHorming) {
        this.missile = missile;
        this.target = target;
        this.range = range;
        this.number = 0;
        this.maxHorming = maxHorming;
        this.isEnd = false;
    }
    
    /**
     * タスクIDを設定する
     * @param id 
     */
    protected void setId(int id) {
        this.id = id;
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
        
        if ( isEnd ) {
            cancelThisTask();
        }
        
        number++;
        
        if ( missile == null || missile.isDead() ) {
            // ミサイルが消滅しているようなのでタスクを終了する
            cancelThisTask();
            return;
        }
        
        if ( target == null || target.isDead() ) {
            // 対象を見失ったので追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            cancelThisTask();
            return;
        }
        
        if ( number > maxHorming ) {
            // 最大ホーミング実行回数を超えたので、追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            cancelThisTask();
            return;
        }
        
        double distance = target.getLocation().distance(missile.getLocation());
        if ( distance > (double)range ) {
            // 距離が離れすぎたため追尾をやめる
            missile.getWorld().playEffect(missile.getLocation(), Effect.POTION_BREAK, 1);
            removeMissile(missile);
            cancelThisTask();
            return;
        }
        
        // 追尾する
        Vector vector = target.getLocation().subtract(missile.getLocation()).toVector();
        double speed = StingerMissile.config.getMissileSpeed();
        vector.normalize().multiply(speed);
        missile.setVelocity(vector);
    }

    /**
     * タスクをキャンセルして終了状態にする
     */
    private void cancelThisTask() {
        Bukkit.getScheduler().cancelTask(id);
        isEnd = true; // set end flag.
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
}
