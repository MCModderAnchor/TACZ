package com.tacz.guns.client.tab;

import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.CustomTabPOJO;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class CustomTabManager {
    //todo 这边需要重新考虑实现
    public static void initAndReload() {
//        clear();
        addCustomTab();
    }

    private static void clear() {
//        int size = 0;
//        for (CreativeModeTab tab : CreativeModeTab.TABS) {
//            if (!(tab instanceof CustomTab)) {
//                size++;
//            }
//        }
//        CreativeModeTab[] tmp = new CreativeModeTab[size];
//        int i = 0;
//        for (CreativeModeTab tab : CreativeModeTab.TABS) {
//            if (!(tab instanceof CustomTab)) {
//                tmp[i] = tab;
//                i++;
//            }
//        }
//        CreativeModeTab.TABS = tmp;
    }

    private static void addCustomTab() {
        Map<String, CustomTabPOJO> customTabs = ClientAssetManager.INSTANCE.getAllCustomTabs();
        for (String key : customTabs.keySet()) {
            CustomTabPOJO data = customTabs.get(key);
            // 连我都觉得奇葩
//            new CustomTab(key, data.getNameKey(), data.getIconStack());
        }
    }
}
