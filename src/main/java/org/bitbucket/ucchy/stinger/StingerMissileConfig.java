/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * コンフィグクラス
 * @author ucchy
 */
public class StingerMissileConfig {

    /** ターゲット取得距離 */
    private int targetingRange;
    
    /** ターゲット取得範囲 */
    private double targetingWidth;
    
    /** ターゲッティングタスクを繰り返す間隔(tick) */
    private int targetingTicks;
    
    /** 最大ターゲット数 */
    private int maxTargetNum;
    
    /** ミサイルのスピード */
    private double missileSpeed;
    
    /** 発射してからホーミング処理を開始させる時間(tick) */
    private int hormingStartTicks;
    
    /** ホーミング処理タスクを繰り返す間隔(tick) */
    private int hormingTicks;
    
    /** ホーミング処理で、ターゲットを見失う距離 */
    private int hormingRange;
    
    /** ホーミング処理を実行する回数 */
    private int hormingNum;
    
    /** 着弾時の爆発威力 */
    private int explosionPower;
    
    /** ミサイルランチャーとして使用するアイテムの素材 */
    private Material launcherMaterial;
    
    /** 無限ミサイルモード */
    private boolean infiniteMissileMode;

    /**
     * コンストラクタ
     */
    public StingerMissileConfig(FileConfiguration config) {
        
        targetingRange = config.getInt("targetingRange", 40);
        targetingWidth = config.getDouble("targetingWidth", 2.5);
        targetingTicks = config.getInt("targetingTicks", 5);
        maxTargetNum = config.getInt("maxTargetNum", 5);
        missileSpeed = config.getDouble("missileSpeed", 1.0);
        hormingStartTicks = config.getInt("hormingStartTicks", 10);
        hormingTicks = config.getInt("hormingTicks", 2);
        hormingRange = config.getInt("hormingRange", 40);
        hormingNum = config.getInt("hormingNum", 45);
        explosionPower = config.getInt("explosionPower", 5);
        infiniteMissileMode = config.getBoolean("infiniteMissileMode", true);
        
        launcherMaterial = Material.getMaterial(config.getString("launcherMaterial", "GOLD_HOE"));
    }

    /**
     * @return targetingRange ターゲット取得距離
     */
    public int getTargetingRange() {
        return targetingRange;
    }

    /**
     * @return targetingWidth ターゲット取得範囲
     */
    public double getTargetingWidth() {
        return targetingWidth;
    }

    /**
     * @return targetingTicks ターゲッティングタスクを繰り返す間隔(tick)
     */
    public int getTargetingTicks() {
        return targetingTicks;
    }

    /**
     * @return maxTargetNum 最大ターゲット数
     */
    public int getMaxTargetNum() {
        return maxTargetNum;
    }

    /**
     * @return missileSpeed ミサイルのスピード
     */
    public double getMissileSpeed() {
        return missileSpeed;
    }

    /**
     * @return hormingStartTicks 発射してからホーミング処理を開始させる時間(tick)
     */
    public int getHormingStartTicks() {
        return hormingStartTicks;
    }

    /**
     * @return hormingTicks ホーミング処理タスクを繰り返す間隔(tick)
     */
    public int getHormingTicks() {
        return hormingTicks;
    }

    /**
     * @return hormingRange ホーミング処理で、ターゲットを見失う距離
     */
    public int getHormingRange() {
        return hormingRange;
    }

    /**
     * @return hormingNum ホーミング処理を実行する回数
     */
    public int getHormingNum() {
        return hormingNum;
    }

    /**
     * @return explosionPower 着弾時の爆発威力
     */
    public int getExplosionPower() {
        return explosionPower;
    }
    
    /**
     * @return launcherMaterial ミサイルランチャーとして使用するアイテムの素材
     */
    public Material getLauncherMaterial() {
        return launcherMaterial;
    }
    
    /**
     * @return infiniteMissileMode 無限ミサイルモード
     */
    public boolean isInfiniteMissileMode() {
        return infiniteMissileMode;
    }
}
