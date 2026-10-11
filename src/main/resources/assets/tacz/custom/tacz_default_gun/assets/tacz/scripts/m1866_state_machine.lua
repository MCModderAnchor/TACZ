-- M1866 杠杆步枪状态机
-- 继承自 default_state_machine，参考 m1897 的 bolt_caught 设计
-- 逐发装填，弹仓 5 发 + 枪膛 1 发，共 6 发
local default = require("tacz_default_state_machine_v2")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local BOLT_CAUGHT_TRACK = default.BOLT_CAUGHT_TRACK
local main_track_states = default.main_track_states

-- 高于所有默认轨道的击锤轨道，确保 static_hammer 不被 MAIN_TRACK 的 bolt/reload 动画覆盖
local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end
local HAMMER_TRACK = increment(default.static_track_top)

local idle_state = setmetatable({}, {__index = main_track_states.idle})

local function isNoAmmo(context)
    return (not context:hasBulletInBarrel()) and (context:getAmmoCount() <= 0)
end

local function getEjectionTime(context)
    local ejection_time = context:getStateMachineParams().bolt_shell_ejecting_time
    if (ejection_time) then
        return ejection_time * 1000
    else
        return 0
    end
end

-- 拉杠杆抛壳状态
local bolt_state = {
    timestamp = -1,
    ejection_time = 0,
    popped = false
}

-- 逐发装填状态
local reload_state = {
    need_ammo = 0,
    loaded_ammo = 0,
    was_empty = false,
    phase = ""
}

-- 装填结束状态（含 reload_end_empty 抛壳）
local ending_state = {
    was_empty = false,
    timestamp = -1,
    ejection_time = 0,
    duration = 0,
    popped = false
}

-- idle_state
function idle_state.transition(this, context, input)
    if (input == INPUT_BOLT) then
        idle_state.hammer_down = false
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK))
        context:runAnimation("bolt", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.bolt
    end
    if (input == INPUT_SHOOT) then
        idle_state.hammer_down = true
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
        return this.main_track_states.idle
    end
    if (input == INPUT_RELOAD) then
        return this.main_track_states.reload
    end
    return main_track_states.idle.transition(this, context, input)
end

-- idle 状态每帧维护击锤状态（进存档时也能正确显示）
function idle_state.update(this, context)
    if (idle_state.hammer_down) then
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
    end
end

-- bolt_state
function bolt_state.entry(this, context)
    local state = this.main_track_states.bolt
    state.ejection_time = getEjectionTime(context)
    state.timestamp = context:getCurrentTimestamp()
    state.popped = false
end

function bolt_state.update(this, context)
    local state = this.main_track_states.bolt
    if (state.popped) then
        return
    end
    if (context:getCurrentTimestamp() - state.timestamp > state.ejection_time) then
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

-- reload_state
function reload_state.entry(this, context)
    local state = this.main_track_states.reload
    state.was_empty = isNoAmmo(context)
    state.need_ammo = context:getMaxAmmoCount() - context:getAmmoCount()
    state.loaded_ammo = 0
    state.phase = "intro"

    context:runAnimation("reload_intro", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0.2)
end

function reload_state.update(this, context)
    local state = this.main_track_states.reload
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)

    -- 空仓换弹时每帧维持 static_hammer，防止框架在弹仓有弹后自动清除
    if (state.was_empty) then
        context:runAnimation("static_hammer", context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK), false, LOOP, 0)
    end

    if (state.phase == "intro") then
        if (not context:isHolding(track)) then
            return
        end
        if (state.need_ammo == 0) then
            context:trigger(this.INPUT_RELOAD_RETREAT)
        elseif (state.need_ammo == 1) then
            context:runAnimation("reload_loop_1_load", track, false, PLAY_ONCE_HOLD, 0)
            state.loaded_ammo = 1
            state.phase = "one_load"
        else
            context:runAnimation("reload_loop_intro", track, false, PLAY_ONCE_HOLD, 0)
            state.loaded_ammo = 1
            state.phase = "loop_intro"
        end
        return
    end

    if (state.phase == "loop_intro" or state.phase == "loop_next" or state.phase == "loop_end" or state.phase == "one_load") then
        if (not context:isHolding(track)) then
            return
        end
    end

    if (state.loaded_ammo >= state.need_ammo or not context:hasAmmoToConsume()) then
        context:trigger(this.INPUT_RELOAD_RETREAT)
        return
    end

    if (state.need_ammo - state.loaded_ammo == 1) then
        context:runAnimation("reload_loop_end", track, false, PLAY_ONCE_HOLD, 0)
    else
        context:runAnimation("reload_loop_next", track, false, PLAY_ONCE_HOLD, 0)
    end
    state.loaded_ammo = state.loaded_ammo + 1
    if (state.need_ammo - state.loaded_ammo == 0) then
        state.phase = "loop_end"
    else
        state.phase = "loop_next"
    end
end

function reload_state.transition(this, context, input)
    if (input == this.INPUT_RELOAD_RETREAT or input == INPUT_CANCEL_RELOAD) then
        return this.main_track_states.ending
    end
    return this.main_track_states.idle.transition(this, context, input)
end

-- ending_state: handles reload_end / reload_end_empty playback and shell ejection
function ending_state.entry(this, context)
    local state = this.main_track_states.ending
    state.was_empty = this.main_track_states.reload.was_empty
    state.timestamp = context:getCurrentTimestamp()
    state.popped = false

    local params = context:getStateMachineParams()
    if (state.was_empty) then
        idle_state.hammer_down = false
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, HAMMER_TRACK))
        context:runAnimation("reload_end_empty", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        local ejTime = params.ending_shell_ejecting_time
        state.ejection_time = ejTime and ejTime * 1000 or 0
        state.duration = params.ending_empty_duration * 1000
    else
        context:runAnimation("reload_end", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        state.duration = params.ending_duration * 1000
    end
end

function ending_state.update(this, context)
    local state = this.main_track_states.ending
    if (state.was_empty and not state.popped) then
        if (context:getCurrentTimestamp() - state.timestamp > state.ejection_time) then
            context:popShellFrom(0)
            state.popped = true
        end
    end
    if (context:getCurrentTimestamp() - state.timestamp < state.duration) then
        return
    end
    context:trigger(this.INPUT_ENDING_FINISH)
end

function ending_state.transition(this, context, input)
    if (input == this.INPUT_ENDING_FINISH) then
        return this.main_track_states.idle
    end
    return this.main_track_states.idle.transition(this, context, input)
end

local M = setmetatable({
    main_track_states = setmetatable({
        idle = idle_state,
        bolt = bolt_state,
        reload = reload_state,
        ending = ending_state
    }, {__index = main_track_states}),
    INPUT_BOLT_RETREAT = "bolt_retreat",
    INPUT_RELOAD_RETREAT = "reload_retreat",
    INPUT_ENDING_FINISH = "ending_finish"
}, {__index = default})

function M:initialize(context)
    default.initialize(self, context)
    -- 进存档时根据枪膛是否为空初始化击锤状态（空膛则击锤落下）
    idle_state.hammer_down = not context:hasBulletInBarrel()
    self.main_track_states.reload.need_ammo = 0
    self.main_track_states.reload.loaded_ammo = 0
    self.main_track_states.reload.was_empty = false
    self.main_track_states.reload.phase = ""
    self.main_track_states.ending.was_empty = false
    self.main_track_states.ending.timestamp = -1
    self.main_track_states.ending.ejection_time = 0
    self.main_track_states.ending.duration = 0
    self.main_track_states.ending.popped = false
end

-- 杠杆枪没有空仓挂机，去掉默认的 bolt_caught_states（用 static_hammer 替代）
function M:states()
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
