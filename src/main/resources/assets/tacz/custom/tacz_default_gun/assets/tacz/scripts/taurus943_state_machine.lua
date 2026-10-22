local default = require("tacz_taurus500_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local main_track_states = default.main_track_states
-- main_track_states.idle 是我们要重写的状态。
local idle_state = setmetatable({}, {__index = main_track_states.idle})
-- reload_state 是定义的新状态，用于把子弹逐发装填进弹巢
local BOLT_CAUGHT_TRACK = default.BOLT_CAUGHT_TRACK
local bolt_caught_states = default.bolt_caught_states

local normal_state = setmetatable({}, {__index = bolt_caught_states.normal})

local reload_state = {
    need_ammo = 0,
    loaded_ammo = 0
}

-- 重写 idle 状态的 transition 函数，将输入 INPUT_RELOAD 重定向到新定义的 reload_state 状态
function idle_state.transition(this, context, input)
    if (input == INPUT_RELOAD) then
        return this.main_track_states.reload
    end
    return main_track_states.idle.transition(this, context, input)
end
-- 在 entry 函数里播放换弹 intro（左轮只有一套换弹流程，不分空仓/战术），
-- 并初始化需要装填的弹药数与已装填的弹药数，后者决定后续 'reload_loop' 循环几次。
function reload_state.entry(this, context)
    local state = this.main_track_states.reload
    context:runAnimation("reload_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
    state.need_ammo = context:getMaxAmmoCount()
    state.loaded_ammo = 0
end
-- 在 update 函数里，循环播放 loop，让 loaded_ammo 变量自增。
function reload_state.update(this, context)
    local state = this.main_track_states.reload
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

-- 更新"不空挂"状态：把弹巢剩余弹药数映射到 static_ammo_display 的时间轴上
function normal_state.update(this, context)
    local a = context:getAmmoCount()
    -- 动画每 0.5s 对应一个弹巢位，满弹为 0.1s；超过 8 发时按满弹显示
    if (a > 8) then
        a = 8
    end
    context:setAnimationProgress(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), 0.1 + (8 - a) * 0.5, false)
end

-- 进入"不空挂"状态
function normal_state.entry(this, context)
    context:runAnimation("static_ammo_display", context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), false, PLAY_ONCE_STOP, 0)
    this.bolt_caught_states.normal.update(this, context)
end

-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    main_track_states = setmetatable({
        -- 自定义的 idle 状态需要覆盖掉父级状态机的对应状态，新建的 reload 状态也要加进来
        idle = idle_state,
        reload = reload_state
    }, {__index = main_track_states}),
    bolt_caught_states = setmetatable({
        normal = normal_state,
    }, {__index = bolt_caught_states}),
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
