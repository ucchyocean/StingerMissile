/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.stinger;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

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

    /** ミサイルランチャーのアイテム表示名 */
    private String launcherDisplayName;

    /** 無限ミサイルモード */
    private boolean infiniteMissileMode;

    /** MOBにターゲッティングするかどうか */
    private boolean targetingToMob;

    /** プレイヤーにターゲッティングするかどうか */
    private boolean targetingToPlayer;

    /** Vehicleにターゲッティングするかどうか */
    private boolean targetingToVehicle;

    /** エンダークリスタルにターゲッティングするかどうか */
    private boolean targetingToEnderCrystal;

    /** ミサイルを打ち出す時に消費する素材 */
    private Material consumeMissileMaterial;

    /** ロックオンまでにかかる遅延サイクル */
    private int lockonDelayCycle;

    /** 重力の打ち消し加速度 */
    private double againstGravity;

    // メッセージ設定

    private String messageTargetCandidate;
    private String messageTargetted;
    private String messageEmptyMissile;

    // サウンド設定

    private SoundComponent soundLockonDelay;
    private SoundComponent soundLockonTarget;
    private SoundComponent soundNoneTarget;
    private SoundComponent soundLaunching;

    /**
     * コンストラクタ
     */
    public StingerMissileConfig(File file) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

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
        targetingToVehicle = config.getBoolean("targetingToVehicle", true);
        targetingToEnderCrystal = config.getBoolean("targetingToEnderCrystal", true);
        againstGravity = config.getDouble("againstGravity", 0.15);

        lockonDelayCycle = config.getInt("lockonDelayCycle", 0);
        if ( lockonDelayCycle < 0 ) {
            lockonDelayCycle = 0;
        }

        launcherMaterial = Material.matchMaterial(config.getString("launcherMaterial", "GOLD_HOE"));
        if ( launcherMaterial == null ) {
            Logger logger = StingerMissile.getInstance().getLogger();
            logger.warning(
                    "Not found material at \"launcherMaterial\" config. The launcher will be GOLD_HOE.");
            launcherMaterial = Material.GOLD_HOE;
        }

        launcherDisplayName = config.getString("launcherDisplayName", "&9&lStinger");

        consumeMissileMaterial =
                Material.matchMaterial(config.getString("consumeMissileMaterial", "ENDER_PEARL"));
        if ( consumeMissileMaterial == null ) {
            Logger logger = StingerMissile.getInstance().getLogger();
            logger.warning(
                    "Not found material at \"consumeMissileMaterial\" config. "
                    + "The consumed item will be ENDER_PEARL.");
            consumeMissileMaterial = Material.ENDER_PEARL;
        }

        messageTargetCandidate = config.getString(
                "messageTargetCandidate",
                "&7target candidate: %name,  distance: %distance");
        messageTargetted = config.getString(
                "messageTargetted", "&6targeted %name. (%num/%max)");
        messageEmptyMissile = config.getString(
                "messageEmptyMissile", "&cYou don''t have missile(%material) !");

        soundLockonDelay = SoundComponent.getComponentFromString(
                config.getString("soundLockonDelay"));
        soundLockonTarget = SoundComponent.getComponentFromString(
                config.getString("soundLockonTarget"));
        soundNoneTarget = SoundComponent.getComponentFromString(
                config.getString("soundNoneTarget"));
        soundLaunching = SoundComponent.getComponentFromString(
                config.getString("soundLaunching"));
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
     * @return launcherDisplayName ミサイルランチャーのアイテム表示名
     */
    public String getLauncherDisplayName() {
        return Utility.replaceColorCode(launcherDisplayName);
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
     * @return targetingToVehicle Vehicleにターゲッティングするかどうか
     */
    public boolean isTargetingToVehicle() {
        return targetingToVehicle;
    }

    /**
     * @return targetingToEnderCrystal エンダークリスタルにターゲッティングするかどうか
     */
    public boolean isTargetingToEnderCrystal() {
        return targetingToEnderCrystal;
    }

    /**
     * @return consumeMissileMaterial ミサイルを打ち出す時に消費する素材
     */
    public Material getConsumeMissileMaterial() {
        return consumeMissileMaterial;
    }

    /**
     * @return lockonDelayCycle
     */
    public int getLockonDelayCycle() {
        return lockonDelayCycle;
    }

    /**
     * @return againstGravity
     */
    public double getAgainstGravity() {
        return againstGravity;
    }

    /**
     * @return messageTargetCandidate
     */
    public String getMessageTargetCandidate(String name, int distance) {
        return Utility.replaceColorCode( messageTargetCandidate
                .replace("%name", name)
                .replace("%distance", distance + "") );
    }

    /**
     * @return messageTargetted
     */
    public String getMessageTargetted(String name, int num, int max) {
        return Utility.replaceColorCode( messageTargetted
                .replace("%name", name)
                .replace("%num", num + "")
                .replace("%max", max + "") );
    }

    /**
     * @return messageEmptyMissile
     */
    public String getMessageEmptyMissile(String material) {
        return Utility.replaceColorCode(
                messageEmptyMissile
                .replace("%material", material) );
    }

    /**
     * @return soundLockonDelay
     */
    public SoundComponent getSoundLockonDelay() {
        return soundLockonDelay;
    }

    /**
     * @return soundLockonTarget
     */
    public SoundComponent getSoundLockonTarget() {
        return soundLockonTarget;
    }

    /**
     * @return soundNoneTarget
     */
    public SoundComponent getSoundNoneTarget() {
        return soundNoneTarget;
    }

    /**
     * @return soundLaunching
     */
    public SoundComponent getSoundLaunching() {
        return soundLaunching;
    }


}
