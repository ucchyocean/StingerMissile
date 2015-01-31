/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.stinger;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * 効果音コンポーネントのパーツ
 * @author ucchy
 */
public class SoundComponentParts {

    private Sound sound;
    private float volume;
    private float pitch;

    private SoundComponentParts(Sound sound) {
        this(sound, 1, 1);
    }

    private SoundComponentParts(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * @return sound
     */
    public Sound getSound() {
        return sound;
    }

    /**
     * @return volume
     */
    public float getVolume() {
        return volume;
    }

    /**
     * @return pitch
     */
    public float getPitch() {
        return pitch;
    }

    public void playSoundToPlayer(Player player) {
        playSoundToPlayer(player, player.getLocation());
    }

    public void playSoundToPlayer(Player player, Location location) {
        player.playSound(location, sound, volume, pitch);
    }

    public void playSoundToWorld(Location location) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public static SoundComponentParts getPartsFromString(String source) {
        String[] t = source.split("-");
        if ( t.length <= 0 ) return null;
        Sound sound = getSoundFromString(t[0]);
        if ( sound == null ) return null;
        float volume = 1;
        float pitch = 1;
        if ( t.length >= 2 ) volume = tryToParseDouble(t[1]);
        if ( t.length >= 3 ) pitch = tryToParseDouble(t[2]);
        return new SoundComponentParts(sound, volume, pitch);
    }

    private static Sound getSoundFromString(String source) {
        if ( source == null ) return null;
        for ( Sound s : Sound.values() ) {
            if ( s.name().equalsIgnoreCase(source) ) {
                return s;
            }
        }
        return null;
    }

    private static float tryToParseDouble(String source) {
        if ( source == null ) return 1;
        try {
            float f = Float.parseFloat(source);
            if ( f < 0 ) return 0;
            if ( 2 < f ) return 2;
            return f;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
