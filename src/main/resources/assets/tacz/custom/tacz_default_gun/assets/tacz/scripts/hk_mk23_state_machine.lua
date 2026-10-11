-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
--
-- 本枪是"继承型"状态机，只保留与默认状态机不同的部分，其余全部透传给 tacz_default_state_machine。
-- 本枪（MK23）的轨道常量分配顺序与默认状态机逐值相同，所以直接取默认的常量即可。
--
-- 本枪与默认状态机的差异集中在"单动/双动分模式"上：
--   BURST 实为双动快射，动画用不带后缀的名字（shoot / static_semi / inspect / reload_*）
--   SEMI  实为单动泵动，动画一律带 _pump 后缀，且需要击锤（hammer）配合
-- 另外本枪刻意删掉了整套空挂（bolt_caught）状态——空挂表现改由 base 轨道的
-- static_bolt_caught[_pump] 动画承担，详见下面的 M:states()。
local default = require("tacz_default_state_machine_v2")

-- 轨道常量：本枪用到的这几个直接沿用默认值
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE
local BASE_TRACK = default.BASE_TRACK
local MAIN_TRACK = default.MAIN_TRACK
local PRE_PARALLEL_TRACK_1 = default.PRE_PARALLEL_TRACK_1
local PRE_PARALLEL_TRACK_2 = default.PRE_PARALLEL_TRACK_2

local main_track_states = default.main_track_states

-- 检查当前是否还有弹药
local function isNoAmmo(context)
    -- 这里同时检查了枪管和弹匣
    return (not context:hasBulletInBarrel()) and (context:getAmmoCount() <= 0)
end

-- 播放换弹动画的方法
-- 与默认状态机不同：先按射击模式分，再按扩容等级分。单动模式用 *_pump 系列
local function runReloadAnimation(context)
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    if (isNoAmmo(context)) then
        if (context:getFireMode() == SEMI) then
            if (context:getMagExtentLevel() == 0) then
                context:runAnimation("reload_empty_pump", track, false, PLAY_ONCE_STOP, 0.2)
            elseif (context:getMagExtentLevel() == 1) then
                context:runAnimation("reload_empty_pump_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
            elseif (context:getMagExtentLevel() == 2 or context:getMagExtentLevel() == 3) then
                context:runAnimation("reload_empty_pump_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
            end
        elseif (context:getFireMode() == BURST) then
            if (context:getMagExtentLevel() == 0) then
                context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
            elseif (context:getMagExtentLevel() == 1) then
                context:runAnimation("reload_empty_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
            elseif (context:getMagExtentLevel() == 2 or context:getMagExtentLevel() == 3) then
                context:runAnimation("reload_empty_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
            end
        end
    else
        if (context:getMagExtentLevel() == 0) then
            context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (context:getMagExtentLevel() == 1) then
            context:runAnimation("reload_tactical_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (context:getMagExtentLevel() == 2 or context:getMagExtentLevel() == 3) then
            context:runAnimation("reload_tactical_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
        end
    end
end

-- 播放检视动画的方法，同样按射击模式分。单动模式用 inspect_pump / inspect_empty_pump
local function runInspectAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    -- 根据当前整枪内是否还有弹药决定是播放普通检视还是空枪检视
    if (isNoAmmo(context)) then
        if (context:getFireMode() == SEMI) then
            context:runAnimation("inspect_empty_pump", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (context:getFireMode() == BURST) then
            context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
        end
    elseif (context:getFireMode() == BURST) then
        context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
    elseif (context:getFireMode() == SEMI) then
        context:runAnimation("inspect_pump", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

-- ===================================================================================== ↓ 基态 ↓ ============
-- 基态完全覆写：默认状态机只有单一的 static_idle，本枪需要按射击模式分，
-- 且空挂表现也做在这里（默认状态机是单独一套 bolt_caught 状态，本枪不用）。
-- mode 记录上一次的射击模式（0 = SEMI，1 = BURST），用于判断要不要播切换模式的过渡动画。
local base_track_state = {
    mode = 0
}

function base_track_state.entry(this, context)
    if (context:getFireMode() == SEMI) then
        base_track_state.mode = 0
        if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
            context:runAnimation("static_bolt_caught_pump", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
        else
            context:runAnimation("static_pump", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
        end
    elseif (context:getFireMode() == BURST) then
        base_track_state.mode = 1
        if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
            context:runAnimation("static_bolt_caught", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
        else
            context:runAnimation("static_semi", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
        end
    end
end

-- 默认状态机的 base_track_state 没有 update，本枪需要它来做两件事：泵动模式的击锤、模式切换过渡
function base_track_state.update(this, context)
    local track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    -- 处理泵动模式下的击锤
    if (context:getFireMode() == SEMI) then
        if (not context:hasBulletInBarrel()) then
            context:runAnimation("shoot_pump_hammer", context:getTrack(STATIC_TRACK_LINE, PRE_PARALLEL_TRACK_1), false, LOOP, 0)
        else
            context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, PRE_PARALLEL_TRACK_1))
        end
    end
    -- 处理切换模式的动画
    if (context:isHolding(track)) then
        if (context:getFireMode() == SEMI) then
            if (base_track_state.mode == 1) then
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("switch_pump_empty", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("switch_pump", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
                base_track_state.mode = 0
            else
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("static_bolt_caught_pump", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("static_pump", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
            end
        elseif (context:getFireMode() == BURST) then
            if (base_track_state.mode == 0) then
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("switch_semi_empty", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("switch_semi", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
                base_track_state.mode = 1
            else
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("static_bolt_caught", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("static_semi", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
            end
        end
    end
end

-- ===================================================================================== ↓ 主轨道 ↓ ============
-- 只覆写"分模式"的四个输入，其余（丢枪、近战）原样转发给默认实现
local idle_state = setmetatable({}, {__index = main_track_states.idle})

function idle_state.transition(this, context, input)
    -- 换弹：必须自己拦下来，否则会走默认的不分模式版本
    if (input == INPUT_RELOAD) then
        runReloadAnimation(context)
        return this.main_track_states.idle
    end
    -- 射击：单动模式由拉栓完成抽壳，不抛壳
    if (input == INPUT_SHOOT) then
        if (context:getFireMode() == BURST) then
            context:popShellFrom(0)
        end
        -- 这里不播放射击动画是因为射击动画应该在 gun_kick 状态里播
        return this.main_track_states.idle
    end
    -- 拉栓：仅单动模式需要，并且叠加播放一遍击锤动画
    if (input == INPUT_BOLT) then
        if (context:getFireMode() == SEMI) then
            context:runAnimation("bolt", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
            context:runAnimation("bolt_hammer", context:getTrack(STATIC_TRACK_LINE, PRE_PARALLEL_TRACK_2), false, PLAY_ONCE_STOP, 0.2)
        end
        return this.main_track_states.idle
    end
    -- 检视：同样必须自己拦下来
    if (input == INPUT_INSPECT and context:getAimingProgress() < 1) then
        runInspectAnimation(context)
        return this.main_track_states.inspect
    end
    -- 丢枪、近战等交回默认实现
    return main_track_states.idle.transition(this, context, input)
end

-- ===================================================================================== ↓ 射击态 ↓ ============
-- 只覆写 transition：按射击模式与是否空仓选三套后座动画
local gun_kick_state = setmetatable({}, {__index = default.gun_kick_state})

function gun_kick_state.transition(this, context, input)
    -- 玩家按下开火键时需要在射击轨道行里寻找空闲轨道去播放射击动画(如果没有空闲会分配新的),需要注意的是射击动画要向下混合
    if (input == INPUT_SHOOT) then
        local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
        if ((context:getAmmoCount() == 0) and (context:getFireMode() == BURST)) then
            -- 双动模式打空仓的最后一发
            context:runAnimation("shoot_last", track, true, PLAY_ONCE_STOP, 0)
        elseif (context:getFireMode() == SEMI) then
            context:runAnimation("shoot_pump", track, true, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("shoot", track, true, PLAY_ONCE_STOP, 0)
        end
    end
    return nil
end

local M = setmetatable({
    main_track_states = setmetatable({
        -- 覆写：按射击模式分动画
        idle = idle_state,
        -- 下面三个是为了隔离父级的共享表，避免跨枪串味：
        -- default.initialize 会无条件写 charge.can_charge，不建自己的表就会原地写父表
        charge = {},
        -- isfinal 会被滑铲门控读取（多个枪械脚本里都有 not isCrawl() or ...isfinal == 1 的判断），
        -- 写进共享表会影响别的枪
        final = { isfinal = -1 },
        allow_run_overlay = false
    }, {__index = main_track_states}),
    -- 覆写：分模式
    base_track_state = base_track_state,
    gun_kick_state = gun_kick_state
}, {__index = default})

-- 状态机初始化函数，在切枪的时候调用
function M:initialize(context)
    default.initialize(self, context)
end

function M:states()
    -- 注意本枪刻意不列出 bolt_caught_states.normal：空挂表现走 base 轨道的
    -- static_bolt_caught[_pump]，若同时注册默认空挂态，会在 BOLT_CAUGHT_TRACK 上多叠一层动画
    return {
        self.base_track_state,
        self.over_heat_states.normal,
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle,
        self.ADS_states.normal,
        self.slide_states.normal
    }
end

return M
