-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_manual_action_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local main_track_states = default.main_track_states
-- main_track_states.idle 是我们要重写的状态。
local idle_state = setmetatable({}, {__index = main_track_states.idle})
-- reload_state、bolt_state 是定义的新状态，用于执行单发装填
local reload_state = {
    need_ammo = 0,
    loaded_ammo = 0
}
-- 本地动画先于服务端确认启动；拒绝或丢失确认时留出 5 秒后收尾。
local RELOAD_CONFIRM_TIMEOUT = 5000
local function isValidUnloadTime(value)
    return type(value) == "number" and value >= 0 and value * 1000 < math.huge
end
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
    if (not context:hasBulletInBarrel() and context:getAmmoCount() <= 0) then
        context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
    elseif (context:getAmmoCount() <= 0) then
        context:runAnimation("inspect_01", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

-- 重写 idle 状态的 transition 函数，将输入 INPUT_RELOAD 重定向到新定义的 reload_state 状态
function idle_state.transition(this, context, input)
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
    local param = context:getStateMachineParams()
    local unload_amount = param.unload_amount
    if (param.unload_magazine == false) then
        unload_amount = 0
    elseif (unload_amount == nil) then
        unload_amount = -1
    end
    state.unload_enabled = isValidUnloadTime(param.unload_time) and type(unload_amount) == "number"
            and unload_amount >= -1 and unload_amount <= 2147483647 and unload_amount % 1 == 0
            and (unload_amount ~= 0 or param.unload_barrel ~= false)
    state.seen_reload = false
    state.finish_requested = false
    state.invalid_unload_time = param.unload_time ~= nil and not isValidUnloadTime(param.unload_time)
    state.reload_started_at = context:getCurrentTimestamp()
    local isNoAmmo = not context:hasBulletInBarrel()
    if (state.unload_enabled and param.unload_barrel ~= false) then
        isNoAmmo = true
    end
    if (isNoAmmo) then
        -- 记录开始换弹的时间戳，用于抛出 reload_intro_empty 中的弹壳
        state.timestamp = context:getCurrentTimestamp()
        state.ejection_time = get_ejection_time(context)
        context:runAnimation("reload_intro_empty", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
    else
        state.timestamp = -1
        state.ejection_time = 0
        context:runAnimation("reload_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
    end
    state.need_ammo = context:getMaxAmmoCount() - context:getAmmoCount()
    if (state.unload_enabled) then
        state.need_ammo = unload_amount == -1 and context:getMaxAmmoCount()
                or math.min(context:getMaxAmmoCount(), state.need_ammo + unload_amount)
    end
    state.loaded_ammo = 0
end
-- 在 update 函数里，循环播放 loop，让 loaded_ammo 变量自增。
function reload_state.update(this, context)
    local state = this.main_track_states.reload
    if (state.invalid_unload_time) then
        context:trigger(this.INPUT_RELOAD_RETREAT)
        return
    end
    -- 处理 reload_intro_empty 的抛壳
    if (state.timestamp ~= -1 and context:getCurrentTimestamp() - state.timestamp > state.ejection_time) then
        context:popShellFrom(0)
        state.timestamp = -1
    end
    -- 卸弹可能发生在背包没有备弹时，使用服务端换弹阶段判断收尾，避免提前取消或重复估算。
    if (state.unload_enabled) then
        local reload_type = context:getReloadStateType()
        local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
        if (reload_type == EMPTY_RELOAD_FINISHING or reload_type == TACTICAL_RELOAD_FINISHING
                or (state.seen_reload and reload_type == NOT_RELOADING)
                or (not state.seen_reload and reload_type == NOT_RELOADING
                    and context:getCurrentTimestamp() - state.reload_started_at >= RELOAD_CONFIRM_TIMEOUT)) then
            state.finish_requested = true
        end
        if (reload_type ~= NOT_RELOADING) then
            state.seen_reload = true
        end
        -- 自然结束要等当前装填动作完整播放，不能在入弹结算帧抢切到结束动作。
        if (state.finish_requested) then
            if (context:isHolding(track)) then
                context:trigger(this.INPUT_RELOAD_RETREAT)
            end
            return
        end
        if (state.seen_reload and state.loaded_ammo < state.need_ammo and context:isHolding(track)) then
            context:runAnimation("reload_loop", track, false, PLAY_ONCE_HOLD, 0)
            state.loaded_ammo = state.loaded_ammo + 1
        end
        return
    end
    if (state.loaded_ammo > state.need_ammo or not context:hasAmmoToConsume()) then
        context:trigger(this.INPUT_RELOAD_RETREAT)
    else
        local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
        if (context:isHolding(track)) then
            context:runAnimation("reload_loop", track, false, PLAY_ONCE_HOLD, 0)
            state.loaded_ammo = state.loaded_ammo + 1
        end
    end
end
-- 如果 loop 循环结束或者换弹被打断，退出到 idle 状态。否则由 idle 的 transition 函数决定下一个状态。
function reload_state.transition(this, context, input)
    if (input == this.INPUT_RELOAD_RETREAT or input == INPUT_CANCEL_RELOAD) then
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
        reload = reload_state
    }, {__index = main_track_states}),
    INPUT_RELOAD_RETREAT = "reload_retreat",
}, {__index = default})
-- 先调用父级状态机的初始化函数，然后进行自己的初始化
function M:initialize(context)
    default.initialize(self, context)
    self.main_track_states.reload.need_ammo = 0
    self.main_track_states.reload.loaded_ammo = 0
end
-- 导出状态机
return M
