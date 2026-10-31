-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_manual_action_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local main_track_states = default.main_track_states

local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end
local HAMMER_TRACK = increment(default.static_track_top)

-- main_track_states.idle 是我们要重写的状态。
local idle_state = setmetatable({}, {__index = main_track_states.idle})

local gun_kick_state = setmetatable({}, {__index = default.gun_kick_state})

-- bolt 动画自带击锤关键帧，进入时清空击锤轨道，避免残留的 static_hammer
-- 在高优先级轨道上盖掉关键帧（高速射击时表现为击锤抖动）。
local bolt_state = setmetatable({}, {__index = main_track_states.bolt})

function bolt_state.entry(this, context)
    context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK))
    return main_track_states.bolt.entry(this, context)
end
-- reload_state、bolt_state 是定义的新状态，用于执行单发装填
local reload_state = {
    need_ammo = 0,
    loaded_ammo = 0,
    was_empty = false
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

-- 检查当前是否还有弹药
local function isNoAmmo(context)
    -- 这里同时检查了枪管和弹匣
    return (not context:hasBulletInBarrel()) and (context:getAmmoCount() <= 0)
end

function gun_kick_state.transition(this, context, input)
    if (input == INPUT_SHOOT) then
        local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
        if (context:getAttachment("SCOPE") == "tacz:empty") then
            context:runAnimation("shoot", track, true, PLAY_ONCE_STOP, 0)
        else
            context:runAnimation("shoot_scope", track, true, PLAY_ONCE_STOP, 0)
        end
    end
    return nil
end

-- 重写 idle 状态的 transition 函数，将输入 INPUT_RELOAD 重定向到新定义的 reload_state 状态
function idle_state.transition(this, context, input)
    if (input == INPUT_RELOAD) then
        if (context:getAttachment("SCOPE") == "tacz:empty" and isNoAmmo(context)) then
            context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK))
            idle_state.timestamp = context:getCurrentTimestamp()
            idle_state.ejection_time = get_ejection_time(context)
            context:runAnimation("reload_empty_clip", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
            return this.main_track_states.idle
        end
        return this.main_track_states.reload
    end
    if (input == INPUT_BOLT) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK))
        if (context:getAttachment("SCOPE") ~= "tacz:empty") then
            context:runAnimation("bolt_scope", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.05)
        else
            context:runAnimation("bolt", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.05)
        end
        return this.main_track_states.bolt
    end
    if (input == INPUT_SHOOT) then
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
        -- 返回到 idle，父级 manual_action 的默认实现会绕过抛壳
    end
    return main_track_states.idle.transition(this, context, input)
end

function idle_state.update(this, context)
    -- 空膛时补正击锤状态：卸弹由服务端结算后同步，触发时膛内弹可能尚未刷新，
    -- 这里按实际状态兜底，避免击锤停留在待击位置。
    -- 仅在主轨道空闲时兜底：reload_empty_clip 等一次性动画自身带击锤关键帧，
    -- 且本轨道优先级高于主轨道，提前覆盖会把关键帧盖掉。
    if (isNoAmmo(context) and context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
    end
    if (idle_state.timestamp ~= nil) then
        if (idle_state.timestamp ~= -1 and context:getCurrentTimestamp() - idle_state.timestamp > idle_state.ejection_time) then
            context:popShellFrom(0)
            idle_state.timestamp = -1
        end
    end
end

-- 在 entry 函数里，我们根据情况选择播放 'reload_intro_empty' 或 'reload_intro' 动画，
-- 并初始化 需要的弹药数、已装填的弹药数。这决定了后续的 'loop' 动画进行几次循环。
function reload_state.entry(this, context, input)
    local state = this.main_track_states.reload

    state.need_ammo = context:getMaxAmmoCount() - context:getAmmoCount()
    state.loaded_ammo = 0
    state.was_empty = isNoAmmo(context)

    if (state.was_empty) then
        state.need_ammo = state.need_ammo + 1
        context:runAnimation("reload_intro_empty", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        idle_state.timestamp = context:getCurrentTimestamp()
        idle_state.ejection_time = get_ejection_time(context)
    else
        context:runAnimation("reload_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
    end
end
-- 在 update 函数里，循环播放 loop，让 loaded_ammo 变量自增。
function reload_state.update(this, context)
    local state = this.main_track_states.reload
    if (state.was_empty) then
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
    end
    if (state.loaded_ammo > state.need_ammo or not context:hasAmmoToConsume()) then
        state.was_empty = false
        context:trigger(this.INPUT_RELOAD_RETREAT)
    else
        local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
        if (context:isHolding(track)) then
            context:runAnimation("reload_loop", track, false, PLAY_ONCE_HOLD, 0)
            state.loaded_ammo = state.loaded_ammo + 1
        end
    end
    if (idle_state.timestamp ~= nil) then
        if (idle_state.timestamp ~= -1 and context:getCurrentTimestamp() - idle_state.timestamp > idle_state.ejection_time) then
            context:popShellFrom(0)
            idle_state.timestamp = -1
        end
    end
end
-- 如果 loop 循环结束或者换弹被打断，退出到 idle 状态。否则由 idle 的 transition 函数决定下一个状态。
function reload_state.transition(this, context, input)
    if (input == this.INPUT_RELOAD_RETREAT or input == INPUT_CANCEL_RELOAD) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK))
        context:runAnimation("reload_end", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
    return this.main_track_states.idle.transition(this, context, input)
end
-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    main_track_states = setmetatable({
        -- 自定义的 idle 状态需要覆盖掉父级状态机的对应状态，新建的 reload 状态也要加进来
        idle = idle_state,
        reload = reload_state,
        bolt = bolt_state
    }, {__index = main_track_states}),
    INPUT_RELOAD_RETREAT = "reload_retreat",
    gun_kick_state = gun_kick_state
}, {__index = default})
-- 先调用父级状态机的初始化函数，然后进行自己的初始化
function M:initialize(context)
    default.initialize(self, context)
    self.main_track_states.reload.need_ammo = 0
    self.main_track_states.reload.loaded_ammo = 0
    self.main_track_states.reload.was_empty = false
    -- 进存档时若为空仓，直接把击锤置为落下，避免首帧从待击跳到落下
    if (isNoAmmo(context)) then
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
    end
end
-- 导出状态机
return M
