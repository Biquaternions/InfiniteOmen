package me.biquaternions.infiniteomen.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

public class EffectListener implements Listener {

    private final JavaPlugin plugin;
    private final Method methodGetHandle;

    private final Field fieldTickingEffects;
    private final Field fieldDeferredEffectUpdates;

    @SuppressWarnings("all")
    private EffectListener(final JavaPlugin plugin) throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.plugin = plugin;

        Class<?> classCraftLivingEntity = (Class<? extends LivingEntity>) Class.forName("org.bukkit.craftbukkit.entity.CraftLivingEntity");
        Class<?> classLivingEntity = Class.forName("net.minecraft.world.entity.LivingEntity");

        this.methodGetHandle = classCraftLivingEntity.getDeclaredMethod("getHandle");
        this.fieldTickingEffects = this.findDeclaredFieldFor(classLivingEntity, "tickingEffects");
        this.fieldDeferredEffectUpdates = this.findDeclaredFieldFor(classLivingEntity, "deferredEffectUpdates");

        this.methodGetHandle.setAccessible(true);
        this.fieldTickingEffects.setAccessible(true);
        this.fieldDeferredEffectUpdates.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onEntityPotionEffect(final EntityPotionEffectEvent event) {
        try {
            final Object livingEntity = this.methodGetHandle.invoke(event.getEntity());
            final boolean tickingEffects = (boolean) this.fieldTickingEffects.get(livingEntity);
            if (tickingEffects) {
                this.fieldTickingEffects.set(livingEntity, false);
                final Queue<Runnable> deferredEffectUpdates = (Queue<Runnable>) this.fieldDeferredEffectUpdates.get(livingEntity);
                Runnable update;
                while ((update = deferredEffectUpdates.poll()) != null) {
                    update.run();
                }
            }
        } catch (final IllegalArgumentException | InvocationTargetException | ClassCastException exception) {
            this.plugin.getSLF4JLogger().error("Failed to perform deferred update drain", exception);
        } catch (final IllegalAccessException exception) {
            throw new IllegalStateException("NMS method is not accessible", exception);
        }
    }

    private Field findDeclaredFieldFor(Class<?> clazz, String... names) throws NoSuchFieldException {
        Field field;
        for (final String name : names) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (final NoSuchFieldException exception) {
                continue;
            }
            return field;
        }
        throw new NoSuchFieldException(String.join(", ", names));
    }

    public static EffectListener generateFor(final JavaPlugin plugin) {
        try {
            return new EffectListener(plugin);
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

}
