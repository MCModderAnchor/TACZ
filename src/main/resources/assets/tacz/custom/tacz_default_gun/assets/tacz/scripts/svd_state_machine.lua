-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_default_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local main_track_states = default.main_track_states
local static_track_top = default.static_track_top

local inspect_state = setmetatable({}, {__index = main_track_states.inspect})
local idle_state = setmetatable({}, {__index = main_track_states.idle})


-- 相当于 obj.value++
local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end

-- 检查当前是否还有弹药
local function isNoAmmo(context)
    -- 这里同时检查了枪管和弹匣
    return (not context:hasBulletInBarrel()) and (context:getAmmoCount() <= 0)
end

local function runReloadAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    local ext = context:getMagExtentLevel()
    -- 根据当前整枪内是否还有弹药决定是播放战术换弹还是空枪换弹
    if (isNoAmmo(context)) then
        -- "ext"表示扩容插件等级,"0"代表不装扩容插件
        if (ext == 0) then
            context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 1) then
            context:runAnimation("reload_empty_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 2) then
            context:runAnimation("reload_empty_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 3) then
            context:runAnimation("reload_empty_xmag_3", track, false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
        end
    else
        if (ext == 0) then
            context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 1) then
            context:runAnimation("reload_tactical_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 2) then
            context:runAnimation("reload_tactical_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 3) then
            context:runAnimation("reload_tactical_xmag_3", track, false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
        end
    end
end

local function runInspectAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    local ext = context:getMagExtentLevel()
    -- 根据当前整枪内是否还有弹药决定是播放普通检视还是空枪检视
    if (isNoAmmo(context)) then
        -- "ext"表示扩容插件等级,"0"代表不装扩容插件
        if (ext == 0) then
            context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 1) then
            context:runAnimation("inspect_empty_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 2) then
            context:runAnimation("inspect_empty_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 3) then
            context:runAnimation("inspect_empty_xmag_3", track, false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
        end
    else
        if (ext == 0) then
            context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 1) then
            context:runAnimation("inspect_xmag_1", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 2) then
            context:runAnimation("inspect_xmag_2", track, false, PLAY_ONCE_STOP, 0.2)
        elseif (ext == 3) then
            context:runAnimation("inspect_xmag_3", track, false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
        end
    end
end

function idle_state.transition(this, context, input)
    if (input == INPUT_RELOAD) then
        runReloadAnimation(context)
        return this.main_track_states.idle
    end
    if (input == INPUT_PUT_AWAY) then
        local put_away_time = context:getPutAwayTime()
        -- 此处获取的轨道是位于主轨道行上的主轨道
        local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
        -- 停下切换模式动画
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, SWITCH_MODE_TRACK))
        -- 播放 put_away 动画,并且将其过渡时长设为从上下文里传入的 put_away_time * 0.75
        context:runAnimation("put_away", track, false, PLAY_ONCE_HOLD, put_away_time * 0.75)
        -- 设定动画进度为最后一帧
        context:setAnimationProgress(track, 1, true)
        -- 将动画进度向前拨动 {put_away_time}
        context:adjustAnimationProgress(track, -put_away_time, false)
        return this.main_track_states.final
    end
    if (input == INPUT_INSPECT) then
        runInspectAnimation(context)
        return inspect_state
    end
    return main_track_states.idle.transition(this, context, input)
end



-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    main_track_states = setmetatable({
        idle = idle_state
    }, {__index = main_track_states}),
    FIRE_MODE_TRACK = FIRE_MODE_TRACK,
    SWITCH_MODE_TRACK = SWITCH_MODE_TRACK,
    INPUT_MODE_SEMI = "input_mode_semi",
    INPUT_MODE_BURST = "input_mode_burst",
    INPUT_MODE_AUTO = "input_mode_auto",
    INPUT_MODE_DRAW = "input_mode_draw"
}, {__index = default})
function M:initialize(context)
    default.initialize(self, context)
end
-- 继承默认状态机需要重新初始化状态
function M:states()
    return {
        self.base_track_state,
        self.bolt_caught_states.normal,
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle,
        self.ADS_states.normal,
        self.slide_states.normal
    }
end
-- 导出状态机
return M