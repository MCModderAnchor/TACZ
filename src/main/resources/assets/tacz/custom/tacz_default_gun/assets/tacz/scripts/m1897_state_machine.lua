-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_default_state_machine")
-- 这个状态机是 manual_action 和 m870 的结合物，因此只要继承自默认状态机即可
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local BOLT_CAUGHT_TRACK = default.BOLT_CAUGHT_TRACK
local main_track_states = default.main_track_states
-- main_track_states.idle 是我们要重写的状态。
local idle_state = setmetatable({}, {__index = main_track_states.idle})
-- reload_state、bolt_state 是定义的新状态，用于执行单发装填
local bolt_state = {
    timestamp = -1,
    ejection_time = 0,
    popped = false
}
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
-- 生成随机数，然后用 lastNum 记录上次的随机数，下次生成时避免和上次重复
local lastNum
local function getAnimationNum()
    local newNum
    repeat
        newNum = math.random(1, 5)
    until newNum ~= lastNum
    lastNum = newNum
    return newNum
end

-- 重写 idle 状态的 transition 函数，讲输入 INPUT_RELOAD 重定向到 bolt_state 状态
function idle_state.transition(this, context, input)
    if (input == INPUT_BOLT) then
        local flag = getAnimationNum()
        -- bolt后解除空挂，击锤处于待激发状态
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK))
        context:runAnimation("bolt", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        -- 5种随机叠加动画，用于让bolt多样化，在 bolt_add_1 ~ bolt_add_5 不重复随机播放
        context:runAnimation("bolt_add_" .. flag, context:findIdleTrack(GUN_KICK_TRACK_LINE, false), true, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.bolt
    end
    -- 绕过抛壳，因为手动上膛的枪不会自动抛壳
    if (input == INPUT_SHOOT) then
        -- 射击后空挂，击锤处于激发状态
        context:runAnimation("static_bolt_caught", context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), false, LOOP, 0)
        return this.main_track_states.idle
    end
    -- 输入 INPUT_RELOAD 重定向到新定义的 reload_state 状态
    if (input == INPUT_RELOAD) then
        return this.main_track_states.reload
    end
    return main_track_states.idle.transition(this, context, input)
end
-- 进入 bolt 状态，初始化时间戳和参数
function bolt_state.entry(this, context)
    local state = this.main_track_states.bolt
    local ejection_time = context:getStateMachineParams().bolt_shell_ejecting_time
    if (ejection_time) then
        state.ejection_time = ejection_time * 1000
    else
        state.ejection_time = 0
    end
    state.timestamp = context:getCurrentTimestamp()
    state.popped = false
end
-- 在恰当的时候执行抛壳逻辑，然后退出状态
function bolt_state.update(this, context)
    local state = this.main_track_states.bolt
    if (state.popped) then
        return
    end
    local base_timestamp = state.timestamp
    local current_timestamp = context:getCurrentTimestamp()
    if (current_timestamp - base_timestamp > state.ejection_time) then
        context:popShellFrom(0)
        state.popped = true
        context:trigger(this.INPUT_BOLT_RETREAT)
    end
end
function bolt_state.transition(this, context, input)
    if (input == this.INPUT_BOLT_RETREAT) then
        return this.main_track_states.idle
    end
    return this.main_track_states.idle.transition(this, context, input)
end

-- 在 entry 函数里，我们根据情况选择播放 'reload_intro_empty' 或 'reload_intro' 动画，
-- 并初始化 需要的弹药数、已装填的弹药数。这决定了后续的 'loop' 动画进行几次循环。
function reload_state.entry(this, context)
    local state = this.main_track_states.reload
    local isNoAmmo = not context:hasBulletInBarrel()
    local flag = math.random(1, 10)
    if (isNoAmmo) then
        -- 记录开始换弹的时间戳，用于抛出 reload_intro_empty 中的弹壳
        state.timestamp = context:getCurrentTimestamp()
        state.ejection_time = get_ejection_time(context)
        if (flag <= 9) then
        -- 1/10概率彩蛋
            context:runAnimation("reload_intro_empty", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        else
            context:runAnimation("reload_intro_empty_easter", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
        end
    else
        state.timestamp = -1
        state.ejection_time = 0
        context:runAnimation("reload_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
    end
    state.need_ammo = context:getMaxAmmoCount() - context:getAmmoCount()
    state.loaded_ammo = 0
end
-- 在 update 函数里，循环播放 loop，让 loaded_ammo 变量自增。
function reload_state.update(this, context)
    local state = this.main_track_states.reload
    -- 处理 reload_intro_empty 的抛壳
    if (state.timestamp ~= -1 and context:getCurrentTimestamp() - state.timestamp > state.ejection_time) then
        context:popShellFrom(0)
        state.timestamp = -1
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
        -- 自定义的 idle 状态需要覆盖掉父级状态机的对应状态，新建的 bolt 状态也要加进来
        idle = idle_state,
        bolt = bolt_state,
        reload = reload_state
    }, {__index = main_track_states}),
    INPUT_RELOAD_RETREAT = "reload_retreat",
    INPUT_BOLT_RETREAT = "bolt_retreat"
}, {__index = default})
-- 先调用父级状态机的初始化函数，然后进行自己的初始化
function M:initialize(context)
    default.initialize(self, context)
    self.main_track_states.reload.need_ammo = 0
    self.main_track_states.reload.loaded_ammo = 0
    self.lastNum = 1
end
-- 导出状态机
return M