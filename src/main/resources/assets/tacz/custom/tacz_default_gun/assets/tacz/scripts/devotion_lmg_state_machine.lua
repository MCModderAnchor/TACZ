local default = require("tacz_default_state_machine")
local GUN_KICK_TRACK_LINE = GUN_KICK_TRACK_LINE

local gun_kick_state = setmetatable({
    acceleration_count = 0
}, {__index = default.gun_kick_state})

function gun_kick_state.transition(this, context, input)
    if (input == INPUT_SHOOT) then
        local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
        context:runAnimation("shoot", track, true, PLAY_ONCE_STOP, 0)
        -- 定义最大连射加速数量、每发加速的比例
        local acceleration_max = 7
        local acceleration_ratio = 0.08
        -- 获取上次射击的 timestamp（系统时间，单位毫秒）
        local last_shoot_timestamp = context:getLastShootTimestamp()
        -- 获取当前系统时间
        local current_timestamp = context:getCurrentTimestamp()
        -- 获取枪械的射击间隔，单位毫秒。用于判断是否在连射，也用于调整射击间隔
        local shoot_interval = context:getShootInterval()
        -- 判断是否在连射，给 2 tick 宽容时间
        if (current_timestamp - last_shoot_timestamp < shoot_interval + 100) then
            -- 正在连射，连射次数 +1
            if (gun_kick_state.acceleration_count < acceleration_max) then
                gun_kick_state.acceleration_count = gun_kick_state.acceleration_count + 1
            end
            -- 根据连射次数调整射击间隔
            local total_ratio = gun_kick_state.acceleration_count * acceleration_ratio
            context:adjustClientShootInterval(-total_ratio * shoot_interval)
        else
            -- 没有在连射，需要重置连射次数
            gun_kick_state.acceleration_count = 0
            -- 没有加速，不需要调整射击间隔
        end
    end
    return nil
end

local M = setmetatable({
    gun_kick_state = gun_kick_state
}, {__index = default})

function M:initialize(context)
    default.initialize(self, context)
    gun_kick_state.acceleration_count = 0
end

return M