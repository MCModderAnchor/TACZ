-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_manual_action_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local BLENDING_TRACK_LINE = default.BLENDING_TRACK_LINE
local SLIDE_TRACK = default.SLIDE_TRACK
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

local function get_ejection_time(context)
    local ejection_time = context:getStateMachineParams().reload_empty_shell_ejecting_time
    if (ejection_time) then
        return ejection_time * 1000
    else
        return 0
    end
end

local function runReloadAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    local ext = context:getMagExtentLevel()
    -- 根据当前整枪内是否还有弹药决定是播放战术换弹还是空枪换弹
    if (isNoAmmo(context)) then
        context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

local function runInspectAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    local ext = context:getMagExtentLevel()
    -- 根据当前整枪内是否还有弹药决定是播放普通检视还是空枪检视
    if (isNoAmmo(context)) then
        context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

local crawl_states = {
    draw = {},
    normal = {},
    crawl = {},
    played_animation = 0,
    is_bolting = false
}

function idle_state.transition(this, context, input)
    if (input == INPUT_RELOAD) then
        if (isNoAmmo(context)) then
            idle_state.timestamp = context:getCurrentTimestamp()
            idle_state.ejection_time = get_ejection_time(context)
        end
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
        this.main_track_states.final.isfinal = 1
        -- 丢枪后转到最终态
        return this.main_track_states.final
    end
    if (input == INPUT_INSPECT) then
        runInspectAnimation(context)
        return inspect_state
    end
    if (input == INPUT_BOLT) then
        -- 标记为 bolt，趴下时手臂不归位
        crawl_states.is_bolting = true
    end
    return main_track_states.idle.transition(this, context, input)
end

function idle_state.update(this, context)
    if (idle_state.timestamp ~= nil) then
        if (idle_state.timestamp ~= -1 and context:getCurrentTimestamp() - idle_state.timestamp > idle_state.ejection_time) then
            context:popShellFrom(0)
            idle_state.timestamp = -1
        end
    end
end

function crawl_states.normal.transition(this, context, input)
    -- 趴下时切到趴下状态
    if (context:isCrawl()) then
        return this.crawl_states.crawl
    end
end

function crawl_states.crawl.entry(this, context)
    -- 重置动画与 bolt 标志位
    crawl_states.played_animation = 0
    crawl_states.is_bolting = false
end

function crawl_states.crawl.update(this, context)
    -- 主轨道正在播放动画 且 趴下轨道无动画 时播放脚架单独展开
    if ((not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) and context:isStopped(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        context:runAnimation("crawl_bipod", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.5)
        crawl_states.played_animation = 1
    end
    -- 主轨道无动画 且 趴下轨道无动画 时播放趴下的起手式
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)) and context:isStopped(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        context:runAnimation("crawl_start", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.5)
    end
    -- 主轨道无动画 且 趴下轨道被挂起（其实就是起手式播放完了） 时播放趴下的持续动作
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)) and context:isHolding(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        -- 主轨道没有播放过动画 持续播放趴下动作
        if (crawl_states.played_animation == 0) then
            context:runAnimation("crawl", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
            -- bolt 结束，手臂本就在趴下态，只需清标志
            if (crawl_states.is_bolting) then
                crawl_states.is_bolting = false
            end
            --主轨道播放过动画 用单独的手臂归为动画回到趴下状态并重置标志位
        elseif (crawl_states.played_animation == 1) then
            context:runAnimation("crawl_handup", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
            crawl_states.played_animation = 0
        end
    end
    -- 主轨道正在播放动画 且 趴下轨道被挂起 时将趴下的叠加层清除掉并将标志位置1
    if ((not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) and context:isHolding(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        if (crawl_states.played_animation == 0 and not crawl_states.is_bolting) then
            context:runAnimation("crawl_handdown", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
            crawl_states.played_animation = 1
        end
    end
end

function crawl_states.crawl.transition(this, context, input)
    if(not context:isCrawl() or this.main_track_states.final.isfinal == 1) then
        if (not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
            context:runAnimation("crawl_bipod_end", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("crawl_end", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_STOP, 0.2)
        end
        crawl_states.is_bolting = false
        return this.crawl_states.normal
    end
end

-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    main_track_states = setmetatable({
        idle = idle_state,
        inspect = inspect_state
    }, {__index = main_track_states}),
    FIRE_MODE_TRACK = FIRE_MODE_TRACK,
    SWITCH_MODE_TRACK = SWITCH_MODE_TRACK,
    INPUT_MODE_SEMI = "input_mode_semi",
    INPUT_MODE_BURST = "input_mode_burst",
    INPUT_MODE_AUTO = "input_mode_auto",
    INPUT_MODE_DRAW = "input_mode_draw",
    crawl_states = crawl_states
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
        self.slide_states.normal,
        self.crawl_states.normal
    }
end
-- 导出状态机
return M