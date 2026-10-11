-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_default_state_machine_v2")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local main_track_states = default.main_track_states
-- main_track_states.idle 是我们要重写的状态。
local idle_state = setmetatable({}, {__index = main_track_states.idle})

local charge_state = {
    can_charge = true
}

-- 常态检测（延迟扳机）
function idle_state.update(this, context)
    if (context:isCharging()) then
        if (charge_state.can_charge) then
            context:trigger(this.INPUT_CHARING)
        end
    else
        charge_state.can_charge = true
    end
end

-- 重写 idle 状态的 transition 函数，将输入 INPUT_CHARING 重定向到新定义的 charge_state 状态
function idle_state.transition(this, context, input)
    -- 进入延迟扳机状态
    if (input == this.INPUT_CHARING) then
        context:runAnimation("charge_in", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, 0)
        return this.main_track_states.charge
    end
    return main_track_states.idle.transition(this, context, input)
end

-- 进入延迟扳机状态
function charge_state.update(this, context)
    if (not context:isCharging()) then
        charge_state.can_charge = true
        context:trigger(this.INPUT_CHARING_EXIT)
    end
end

-- 离开延迟扳机状态
function charge_state.transition(this, context, input)
    -- 切换物品可以中断蓄力，收枪动画沿用闲置态的处理
    if (input == INPUT_PUT_AWAY) then
        return main_track_states.idle.transition(this, context, input)
    end
    if (input == INPUT_SHOOT) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
        charge_state.can_charge = false
        return this.main_track_states.idle
    end
    if (input == this.INPUT_CHARING_EXIT) then
        context:runAnimation("charge_out", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.3)
        return this.main_track_states.idle
    end
end

-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    main_track_states = setmetatable({
        idle = idle_state,
        charge = charge_state
    }, {__index = main_track_states}),
    INPUT_RELOAD_RETREAT = "reload_retreat",
    INPUT_CHARING = "input_charging",
    INPUT_CHARING_EXIT = "input_charging_exit"
}, {__index = default})
-- 先调用父级状态机的初始化函数，然后进行自己的初始化
function M:initialize(context)
    default.initialize(self, context)
    self.main_track_states.charge.can_charge = true
end
-- 导出状态机
return M
