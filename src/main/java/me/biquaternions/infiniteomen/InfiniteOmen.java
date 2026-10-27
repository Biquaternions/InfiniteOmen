package me.biquaternions.infiniteomen;

import me.biquaternions.infiniteomen.listener.EffectListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfiniteOmen extends JavaPlugin {

    @Override
    public void onEnable() {

        final EffectListener listener;
        try {
            listener = EffectListener.generateFor(this);
        } catch (final IllegalStateException exception) {
            this.getSLF4JLogger().error("Failed to find NMS binding, check the server version", exception);
            return;
        }

        this.getServer().getPluginManager().registerEvents(listener, this);
    }

}
