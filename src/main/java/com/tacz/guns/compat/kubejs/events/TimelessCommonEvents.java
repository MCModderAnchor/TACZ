package com.tacz.guns.compat.kubejs.events;

import com.tacz.guns.api.event.common.*;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;
import net.minecraftforge.eventbus.api.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TimelessCommonEvents implements TimelessKubeJSEventRegister{
    public static final TimelessCommonEvents INSTANCE = new TimelessCommonEvents();
    public static final Map<Class<? extends Event>, Consumer<Event>> EVENT_HANDLERS = new HashMap<>();
    public static final EventHandler ENTITY_HURT_BY_GUN_PRE = INSTANCE.registerTimelessCommonEvent(
            "entityHurtByGunPre",
            GunKubeJSEvents.EntityHurtByGunPreEventJS.class,
            EntityHurtByGunEvent.Pre.class,
            GunKubeJSEvents.EntityHurtByGunPreEventJS::new,
            true
    );
    public static final EventHandler ENTITY_HURT_BY_GUN_POST = INSTANCE.registerTimelessCommonEvent(
            "entityHurtByGunPost",
            GunKubeJSEvents.EntityHurtByGunPostEventJS.class,
            EntityHurtByGunEvent.Post.class,
            GunKubeJSEvents.EntityHurtByGunPostEventJS::new
    );
    public static final EventHandler ENTITY_KILL_BY_GUN = INSTANCE.registerTimelessCommonEvent(
            "entityKillByGun",
            GunKubeJSEvents.EntityKillByGunEventJS.class,
            EntityKillByGunEvent.class,
            GunKubeJSEvents.EntityKillByGunEventJS::new
    );
    public static final EventHandler GUN_DRAW = INSTANCE.registerTimelessCommonEvent(
            "gunDraw",
            GunKubeJSEvents.GunDrawEventJS.class,
            GunDrawEvent.class,
            GunKubeJSEvents.GunDrawEventJS::new
    );
    public static final EventHandler GUN_FINISH_RELOAD = INSTANCE.registerTimelessCommonEvent(
            "gunFinishReload",
            GunKubeJSEvents.GunFinishReloadEventJS.class,
            GunFinishReloadEvent.class,
            GunKubeJSEvents.GunFinishReloadEventJS::new
    );
    public static final EventHandler GUN_FIRE = INSTANCE.registerTimelessCommonEvent(
            "gunFire",
            GunKubeJSEvents.GunFireEventJS.class,
            GunFireEvent.class,
            GunKubeJSEvents.GunFireEventJS::new,
            true
    );
    public static final EventHandler GUN_FIRE_POST = INSTANCE.registerTimelessCommonEvent(
            "gunFirePost",
            GunKubeJSEvents.GunFirePostEventJS.class,
            GunFirePostEvent.class,
            GunKubeJSEvents.GunFirePostEventJS::new
    );
    public static final EventHandler GUN_FIRE_SELECT = INSTANCE.registerTimelessCommonEvent(
            "gunFireSelect",
            GunKubeJSEvents.GunFireSelectEventJS.class,
            GunFireSelectEvent.class,
            GunKubeJSEvents.GunFireSelectEventJS::new,
            true
    );
    public static final EventHandler GUN_MELEE = INSTANCE.registerTimelessCommonEvent(
            "gunMelee",
            GunKubeJSEvents.GunMeleeEventJS.class,
            GunMeleeEvent.class,
            GunKubeJSEvents.GunMeleeEventJS::new,
            true
    );
    public static final EventHandler GUN_RELOAD = INSTANCE.registerTimelessCommonEvent(
            "gunReload",
            GunKubeJSEvents.GunReloadEventJS.class,
            GunReloadEvent.class,
            GunKubeJSEvents.GunReloadEventJS::new,
            true
    );
    public static final EventHandler GUN_SHOOT = INSTANCE.registerTimelessCommonEvent(
            "gunShoot",
            GunKubeJSEvents.GunShootEventJS.class,
            GunShootEvent.class,
            GunKubeJSEvents.GunShootEventJS::new,
            true
    );

    private TimelessCommonEvents() {}

    @Override
    public Map<Class<? extends Event>, Consumer<Event>> getEventHandlers() {
        return EVENT_HANDLERS;
    }

    @Override
    public <E extends Event> void registerEventHandler(Class<E> eventClass, Consumer<Event> eventPoster) {
        EVENT_HANDLERS.put(eventClass, eventPoster);
    }

    @Override
    public ScriptTypePredicate getScriptType() {
        return ScriptTypePredicate.COMMON;
    }
}
