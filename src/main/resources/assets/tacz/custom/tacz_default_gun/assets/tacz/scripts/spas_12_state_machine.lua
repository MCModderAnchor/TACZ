-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_manual_action_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE
local main_track_states = default.main_track_states
local gun_kick_state = default.gun_kick_state
local ADS_states = default.ADS_states
local BASE_TRACK = default.BASE_TRACK
-- main_track_states.idle 是我们要重写的状态。
local shoot_state = setmetatable({},{__index = gun_kick_state})
local idle_state = setmetatable({}, {__index = main_track_states.idle})
local start_state = setmetatable({}, {__index = main_track_states.start})
local inspect_state = setmetatable({}, {__index = main_track_states.inspect})
-- local caught_state = setmetatable({}, {__index = bolt_caught_states})
-- local base_state = setmetatable({},{__index = base_track_state})
-- reload_state、bolt_state 是定义的新状态，用于执行单发装填

local function runPutAwayAnimation(context)
    local put_away_time = context:getPutAwayTime()
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    -- 播放 put_away 动画,并且将其过渡时长设为从上下文里传入的 put_away_time * 0.75
    if(context:getFireMode() == BURST)then
        if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
            context:runAnimation("put_away_semi_caught", track, false, PLAY_ONCE_HOLD, put_away_time * 0.75)
        else
            context:runAnimation("put_away_semi", track, false, PLAY_ONCE_HOLD, put_away_time * 0.75)
        end
    else
        context:runAnimation("put_away", track, false, PLAY_ONCE_HOLD, put_away_time * 0.75)
    end
    -- 设定动画进度为最后一帧
    context:setAnimationProgress(track, 1, true)
    -- 将动画进度向前拨动 {put_away_time}
    context:adjustAnimationProgress(track, -put_away_time, false)
end

function start_state.transition(this, context, input)
    if (input == INPUT_DRAW) then
        -- 收到 draw 信号后在主轨道行的主轨道上播放掏枪动画,然后转到闲置态
        if(context:getFireMode() == BURST)then
            if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                context:runAnimation("draw_semi_caught", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0)
            else
                context:runAnimation("draw_semi", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0)
            end
        else
            context:runAnimation("draw", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0)
        end
        return this.main_track_states.idle
    end
end

local crosshair_state = {
    ads_ing = 0
}

-- 进入瞄准状态
function crosshair_state.entry(this, context)
    -- 开始瞄准时播放瞄准动画，并且将其挂起
end

-- 更新瞄准状态
function crosshair_state.update(this, context)
    if (context:getAimingProgress() >= 1) then
        crosshair_state.ads_ing = 1
        if (context:getAttachment("SCOPE") == "tacz:empty") then
            context:setShouldHideCrossHair(false)
        else
            context:setShouldHideCrossHair(true)
        end
    else
        if (crosshair_state.ads_ing == 1) then
            crosshair_state.ads_ing = 0
            context:setShouldHideCrossHair(false)
        end
        context:setShouldHideCrossHair(context:shouldHideCrossHair())
    end
end

local base_state = {
    mode = 0
}
-- 进入基础状态,直接播放 static_idle
function base_state.entry(this, context)
    if (context:getFireMode() == SEMI) then
        base_state.mode = 0
        context:runAnimation("static_idle", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
    elseif (context:getFireMode() == BURST) then
        base_state.mode = 1
        if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
            context:runAnimation("static_idle_semi_caught", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
        else
            context:runAnimation("static_idle_semi", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
        end
    end
end

function base_state.update(this, context)
    local track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    if (context:isHolding(track)) then
        if (context:getFireMode() == SEMI) then
            if (base_state.mode == 1) then
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("switch_pump_empty", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("switch_pump", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
                base_state.mode = 0
            else
                context:runAnimation("static_idle", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
            end
        elseif (context:getFireMode() == BURST) then
            if (base_state.mode == 0) then
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("switch_semi_empty", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("switch_semi", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
                base_state.mode = 1
            else
                if ((not context:hasBulletInBarrel()) and context:getAmmoCount() == 0) then
                    context:runAnimation("static_idle_semi_caught", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                else
                    context:runAnimation("static_idle_semi", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, PLAY_ONCE_HOLD, 0)
                end
            end
        end
    end
end

function shoot_state.transition(this, context, input)
    if (input == INPUT_SHOOT) then
        local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
        if (context:getFireMode() == SEMI) then
            context:runAnimation("shoot", track, true, PLAY_ONCE_STOP, 0)
        elseif (context:getFireMode() == BURST) then
            if (context:getAmmoCount() == 0) then
                context:runAnimation("shoot_semi_last", track, true, PLAY_ONCE_STOP, 0)
            else
                context:runAnimation("shoot_semi", track, true, PLAY_ONCE_STOP, 0)
            end
            context:popShellFrom(0)
        end
    end
    return nil
end

local reload_state = {
    need_ammo = 0,
    loaded_ammo = 0
}
local function get_ejection_time(context)
    local ejection_time = context:getStateMachineParams().intro_shell_ejecting_time
    if (ejection_time) then
        ejection_time = ejection_time * 1000
    else
        ejection_time = 0
    end
    return ejection_time
end

local function runInspectAnimation(context)
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    if (not context:hasBulletInBarrel() and context:getAmmoCount() <= 0 and context:getFireMode() == SEMI) then
        context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
    elseif (not context:hasBulletInBarrel() and context:getAmmoCount() <= 0 and context:getFireMode() == BURST) then
        context:runAnimation("inspect_empty_semi", track, false, PLAY_ONCE_STOP, 0.2)
    elseif (context:getAmmoCount() <= 0 and context:getFireMode() == SEMI) then
        context:runAnimation("inspect_1", track, false, PLAY_ONCE_STOP, 0.2)
    elseif (context:getAmmoCount() <= 0 and context:getFireMode() == BURST) then
        context:runAnimation("inspect_1_semi", track, false, PLAY_ONCE_STOP, 0.2)
    elseif (context:getFireMode() == BURST) then
        context:runAnimation("inspect_semi", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

-- 重写 idle 状态的 transition 函数，将输入 INPUT_RELOAD 重定向到新定义的 reload_state 状态
function idle_state.transition(this, context, input)
    if (input == INPUT_PUT_AWAY) then
        runPutAwayAnimation(context)
        -- 丢枪后转到最终态
        return this.main_track_states.final
    end
    if (input == INPUT_RELOAD) then
        return this.main_track_states.reload
    end
    if (input == INPUT_INSPECT) then
        runInspectAnimation(context)
        return this.main_track_states.inspect
    end
    return main_track_states.idle.transition(this, context, input)
end
-- 在 entry 函数里，我们根据情况选择播放 'reload_intro_empty' 或 'reload_intro' 动画，
-- 并初始化 需要的弹药数、已装填的弹药数。这决定了后续的 'loop' 动画进行几次循环。
function reload_state.entry(this, context)
    local state = this.main_track_states.reload
    local isNoAmmo = not context:hasBulletInBarrel()
    if (isNoAmmo) then
        -- 记录开始换弹的时间戳，用于抛出 reload_intro_empty 中的弹壳
        state.timestamp = context:getCurrentTimestamp()
        state.ejection_time = get_ejection_time(context)
        if (context:getFireMode() == BURST) then
            context:runAnimation("reload_empty_intro_semi", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        else
            context:runAnimation("reload_empty_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        end
    else
        state.timestamp = -1
        state.ejection_time = 0
        if (context:getFireMode() == BURST) then
            context:runAnimation("reload_intro_semi", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        else
            context:runAnimation("reload_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        end
    end
    state.need_ammo = context:getMaxAmmoCount() - context:getAmmoCount()
    state.loaded_ammo = 0
end
-- 在 update 函数里，循环播放 loop，让 loaded_ammo 变量自增。
function reload_state.update(this, context)
    local state = this.main_track_states.reload
    -- 处理 reload_intro_empty 的抛壳
    if (state.timestamp ~= -1 and context:getCurrentTimestamp() - state.timestamp > state.ejection_time) then
        if (context:getFireMode() == SEMI) then
            context:popShellFrom(0)
        end
        state.timestamp = -1
    end
    if (state.loaded_ammo > state.need_ammo or not context:hasAmmoToConsume()) then
        context:trigger(this.INPUT_RELOAD_RETREAT)
    else
        local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
        if (context:isHolding(track)) then
            if (context:getFireMode() == BURST) then
                context:runAnimation("reload_loop_semi", track, false, PLAY_ONCE_HOLD, 0)
            else
                context:runAnimation("reload_loop", track, false, PLAY_ONCE_HOLD, 0)
            end
            state.loaded_ammo = state.loaded_ammo + 1
        end
    end
end
-- 如果 loop 循环结束或者换弹被打断，退出到 idle 状态。否则由 idle 的 transition 函数决定下一个状态。
function reload_state.transition(this, context, input)
    if (input == this.INPUT_RELOAD_RETREAT or input == INPUT_CANCEL_RELOAD) then
        if (context:getFireMode() == BURST) then
            context:runAnimation("reload_end_semi", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("reload_end", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        end
        return this.main_track_states.idle
    end
    return this.main_track_states.idle.transition(this, context, input)
end

-- 当检测到开火模式切换输入时应该直接停止动画并返回闲置态
function inspect_state.transition(this, context, input)
    if (input == INPUT_FIRE_SELECT) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
        return this.main_track_states.idle
    end
    return main_track_states.inspect.transition(this, context, input)
end

-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    main_track_states = setmetatable({
        idle = idle_state,
        start = start_state,
        reload = reload_state,
        inspect = inspect_state,
    }, {__index = main_track_states}),
    crosshair_state = crosshair_state,
    INPUT_RELOAD_RETREAT = "reload_retreat",
    gun_kick_state = setmetatable({},{__index = shoot_state}),
    base_track_state = setmetatable({},{__index = base_state})
}, {__index = default})
-- 先调用父级状态机的初始化函数，然后进行自己的初始化
function M:initialize(context)
    default.initialize(self, context)
    self.main_track_states.reload.need_ammo = 0
    self.main_track_states.reload.loaded_ammo = 0
end

function M:states()
    return {
        self.base_track_state,
        self.bolt_caught_states.normal,
        self.over_heat_states.normal,
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle,
        self.ADS_states.normal,
        self.slide_states.normal,
        self.crosshair_state
    }
end
-- 导出状态机
return M