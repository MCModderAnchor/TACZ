package com.tac.guns.event;

import com.google.common.collect.Maps;
import com.tac.guns.config.common.OtherConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class HeadShotAABBConfigRead {
    private static final String CONFIG_NAME = "tac-common.toml";
    private static final Map<ResourceLocation, AABB> AABB_CHECK = Maps.newHashMap();
    // 书写格式：touhou_little_maid:maid [-0.5, 1.0, -0.5, 0.5, 1.5, 0.5]
    // 生物 ID + 碰撞箱
    private static final Pattern REG = Pattern.compile("^([a-z0-9_.-]+:[a-z0-9/._-]+)\s*?\\[([-+]?[0-9]*\\.?[0-9]+),\s*?([-+]?[0-9]*\\.?[0-9]+),\s*?([-+]?[0-9]*\\.?[0-9]+),\s*?([-+]?[0-9]*\\.?[0-9]+),\s*?([-+]?[0-9]*\\.?[0-9]+),\s*?([-+]?[0-9]*\\.?[0-9]+),*?\s*?]");

    @SubscribeEvent
    public static void onEvent(ModConfigEvent.Loading event) {
        String fileName = event.getConfig().getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            AABB_CHECK.clear();
            List<String> configData = OtherConfig.HEAD_SHOT_AABB.get();
            for (String text : configData) {
                addCheck(text);
            }
            System.out.println(AABB_CHECK);
        }
    }

    public static void addCheck(String text) {
        Matcher matcher = REG.matcher(text);
        if (matcher.find()) {
            ResourceLocation id = new ResourceLocation(matcher.group(1));
            double x1 = Double.parseDouble(matcher.group(2));
            double y1 = Double.parseDouble(matcher.group(3));
            double z1 = Double.parseDouble(matcher.group(4));
            double x2 = Double.parseDouble(matcher.group(5));
            double y2 = Double.parseDouble(matcher.group(6));
            double z2 = Double.parseDouble(matcher.group(7));
            AABB aabb = new AABB(x1, y1, z1, x2, y2, z2);
            AABB_CHECK.put(id, aabb);
        }
    }

    public static void clearAABB() {
        AABB_CHECK.clear();
    }

    public static AABB getAABB(ResourceLocation id) {
        return AABB_CHECK.get(id);
    }
}
