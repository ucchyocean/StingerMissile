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

    /** ミサイルの加速スピード */
    private double missileAccelSpeed;

    /** ミサイルの最大スピード */
    private double missileMaxSpeed;

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

    /** 着弾時のダメージ量 */
    private double explosionDamage;

    /** ミサイルランチャーとして使用するアイテムの素材 */
    private Material launcherMaterial;

    /** 無限ミサイルモード */
    private boolean infiniteMissileMode;

    /** MOBにターゲッティングするかどうか */
    private boolean targetingToMob;

    /** プレイヤーにターゲッティングするかどうか */
    private boolean targetingToPlayer;

    /** ミサイルを打ち出す時に消費する素材 */
    private Material consumeMissileMaterial;

    /**
     * コンストラクタ
     */
    public StingerMissileConfig(FileConfiguration config) {

        targetingRange = config.getInt("targetingRange", 40);
        targetingWidth = config.getDouble("targetingWidth", 2.5);
        targetingTicks = config.getInt("targetingTicks", 5);
        maxTargetNum = config.getInt("maxTargetNum", 5);
        missileAccelSpeed = config.getDouble("missileAccelSpeed", 0.4);
        missileMaxSpeed = config.getDouble("missileMaxSpeed", 2.0);
        hormingStartTicks = config.getInt("hormingStartTicks", 10);
        hormingTicks = config.getInt("hormingTicks", 2);
        hormingRange = config.getInt("hormingRange", 40);
        hormingNum = config.getInt("hormingNum", 45);
        explosionPower = config.getInt("explosionPower", 3);
        explosionDamage = config.getDouble("explosionDamage", 3.0);
        infiniteMissileMode = config.getBoolean("infiniteMissileMode", true);
        targetingToMob = config.getBoolean("targetingToMob", true);
        targetingToPlayer = config.getBoolean("targetingToPlayer", true);

        launcherMaterial = Material.matchMaterial(config.getString("launcherMaterial", "GOLD_HOE"));
        if ( launcherMaterial == null ) {
            StingerMissile.instance.getLogger().warning(
                    "Not found material at \"launcherMaterial\" config. The launcher will be GOLD_HOE.");
            launcherMaterial = Material.GOLD_HOE;
        }

        consumeMissileMaterial =
                Material.matchMaterial(config.getString("consumeMissileMaterial", "ENDER_PEARL"));
        if ( consumeMissileMaterial == null ) {
            StingerMissile.instance.getLogger().warning(
                    "Not found material at \"consumeMissileMaterial\" config. "
                    + "The consumed item will be ENDER_PEARL.");
            consumeMissileMaterial = Material.ENDER_PEARL;
        }
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
     * @return missileAccelSpeed ミサイルの加速スピード
     */
    public double getMissileAccelSpeed() {
        return missileAccelSpeed;
    }

    /**
     * @return missileAccelSpeed ミサイルの最大スピード
     */
    public double getMissileMaxSpeed() {
        return missileMaxSpeed;
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
     * @return explosionPower 着弾時のダメージ威力
     */
    public double getExplosionDamage() {
        return explosionDamage;
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

    /**
     * @return targetingToMob MOBにターゲッティングするかどうか
     */
    public boolean isTargetingToMob() {
        return targetingToMob;
    }

    /**
     * @return targetingToPlayer プレイヤーにターゲッティングするかどうか
     */
    public boolean isTargetingToPlayer() {
        return targetingToPlayer;
    }

    /**
     * @return consumeMissileMaterial ミサイルを打ち出す時に消費する素材
     */
    public Material getConsumeMissileMaterial() {
        return consumeMissileMaterial;
    }
}
