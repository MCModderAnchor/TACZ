package com.tacz.guns.client.model;

public final class GunModelConstant {
    /**
     * 枪管中的子弹，用于闭膛待击枪械的渲染，枪管中没有子弹时隐藏该组
     */
    public static final String BULLET_IN_BARREL = "bullet_in_barrel";
    /**
     * 弹匣内的子弹，会在弹匣打空时隐藏该组
     */
    public static final String BULLET_IN_MAG = "bullet_in_mag";
    /**
     * 弹链，多用于机枪，在子弹打空时隐藏
     */
    public static final String BULLET_CHAIN = "bullet_chain";
    /**
     * 无瞄具时可见，通常用于 M4 上
     */
    public static final String CARRY = "carry";
    /**
     * 安装一级扩容弹匣时显示
     */
    public static final String MAG_EXTENDED_1 = "mag_extended_1";
    /**
     * 安装二级扩容弹匣时显示
     */
    public static final String MAG_EXTENDED_2 = "mag_extended_2";
    /**
     * 安装三级扩容弹匣时显示
     */
    public static final String MAG_EXTENDED_3 = "mag_extended_3";
    /**
     * 没有安装扩容弹匣时显示
     */
    public static final String MAG_STANDARD = "mag_standard";
    /**
     * 有瞄具时显示，用于放瞄具的导轨（如 AKM 的导轨）
     */
    public static final String MOUNT = "mount";
    /**
     * 无瞄具时可见，机械瞄具
     */
    public static final String SIGHT = "sight";
    /**
     * 有瞄具时显示，折叠的机械瞄具
     */
    public static final String SIGHT_FOLDED = "sight_folded";
    /**
     * 可以被理解为：在玩家用枪械的机械瞄具瞄准时，玩家眼球的位置和朝向
     */
    public static final String IRON_VIEW_NODE = "iron_view";
    /**
     * 玩家在非瞄准状态下眼球的位置和朝向
     */
    public static final String IDLE_VIEW_NODE = "idle_view";
    /**
     * 默认的改装界面定位组
     */
    public static final String REFIT_VIEW_NODE = "refit_view";
    /**
     * 第三人称枪械定位组
     */
    public static final String THIRD_PERSON_HAND_ORIGIN_NODE = "thirdperson_hand";
    /**
     * 展示框定位组
     */
    public static final String FIXED_ORIGIN_NODE = "fixed";
    /**
     * 掉落物定位组
     */
    public static final String GROUND_ORIGIN_NODE = "ground";
    /**
     * 抛壳起点定位组
     */
    public static final String SHELL_ORIGIN_NODE = "shell";
    public static final String SHELL_ORIGIN_NODE_PREFIX = "shell_";
    /**
     * 枪口火焰定位组
     */
    public static final String MUZZLE_FLASH_ORIGIN_NODE = "muzzle_flash";
    /**
     * 第一人称左手手臂组
     */
    public static final String LEFTHAND_POS_NODE = "lefthand_pos";
    /**
     * 第一人称右手手臂组
     */
    public static final String RIGHTHAND_POS_NODE = "righthand_pos";
    /**
     * 弹匣定位组
     */
    public static final String MAG_NORMAL_NODE = "magazine";
    /**
     * 换弹时第二个弹匣定位组
     */
    public static final String MAG_ADDITIONAL_NODE = "additional_magazine";
    /**
     * 配件转接口
     */
    public static final String ATTACHMENT_ADAPTER_NODE = "attachment_adapter";
    /**
     * 默认护木
     */
    public static final String HANDGUARD_DEFAULT_NODE = "handguard_default";
    /**
     * 战术护木
     */
    public static final String HANDGUARD_TACTICAL_NODE = "handguard_tactical";
    /**
     * 配件定位组后缀，实际名称为配件名（小写）加上这个
     */
    public static final String ATTACHMENT_POS_SUFFIX = "_pos";
    /**
     * 默认配件组后缀，会在安装配件后隐藏，实际名称为配件名（小写）加上这个
     */
    public static final String DEFAULT_ATTACHMENT_SUFFIX = "_default";
    /**
     * 改装界面视角的定位组前缀，实际名称为：前缀 + 配件名（小写）+ 后缀
     */
    public static final String REFIT_VIEW_PREFIX = "refit_";
    /**
     * 改装界面视角的定位组后缀，实际名称为：前缀 + 配件名（小写）+ 后缀
     */
    public static final String REFIT_VIEW_SUFFIX = "_view";
    /**
     * 根组
     */
    public static final String ROOT_NODE = "root";
}