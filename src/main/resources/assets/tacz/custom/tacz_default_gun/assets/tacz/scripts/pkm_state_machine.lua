-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_default_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local BLENDING_TRACK_LINE = default.BLENDING_TRACK_LINE
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE
local BOLT_CAUGHT_TRACK = default.BOLT_CAUGHT_TRACK
local SLIDE_TRACK = default.SLIDE_TRACK
local MAIN_TRACK = default.MAIN_TRACK
local bolt_caught_states = default.bolt_caught_states
local main_track_states = default.main_track_states
local static_track_top = default.static_track_top
local main_states = setmetatable({}, {__index = main_track_states.idle})

local normal_state = setmetatable({}, {__index = bolt_caught_states.normal})

-- 栈顶指针自增函数
local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end

-- 弹链显示轨道
local BELT_TRACK = increment(static_track_top)

-- ========== 弹链显示状态 ==========
local belt_state = {
    belt_count = 0,                      -- 累计开火次数
    max_count = 11,                      -- 弹链容量
    is_reloading = false,                -- 换弹中，等待延迟清零
    reload_delay = 0,                    -- 当前清零延迟（毫秒）
    reload_start_time = 0,               -- 换弹起始时间戳
    -- 弹匣移出屏幕时机（毫秒）
    reload_tactical_delay = 3800,         -- 战术换弹延迟
    reload_empty_delay = 3400,            -- 空仓换弹延迟
}

function belt_state.entry(this, context)
    belt_state.belt_count = 0
    belt_state.is_reloading = false
    context:runAnimation("static_belt_display", context:getTrack(STATIC_TRACK_LINE, BELT_TRACK), false, PLAY_ONCE_HOLD, 0)
end

function belt_state.update(this, context)
    -- 换弹延迟清零：累计时间达标时归零弹链计数
    if (belt_state.is_reloading) then
        local elapsed = context:getCurrentTimestamp() - belt_state.reload_start_time
        if (elapsed >= belt_state.reload_delay) then
            belt_state.belt_count = 0
            belt_state.is_reloading = false
        end
    end
    -- 弹链进度 = base + count * step
    -- count=0 时空链全满, count=max 时空弹全空
    -- 修改下面公式里的数值来控制弹链消失速度和起始位置
    local a = belt_state.belt_count
    context:setAnimationProgress(context:getTrack(STATIC_TRACK_LINE, BELT_TRACK), 0.1 + a * 0.5, false)
end

-- ========== 辅助方法 ==========

-- 播放丢枪动画
local function runPutAwayAnimation(context)
    local put_away_time = context:getPutAwayTime()
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    context:runAnimation("put_away", track, false, PLAY_ONCE_HOLD, put_away_time * 0.75)
    context:setAnimationProgress(track, 1, true)
    context:adjustAnimationProgress(track, -put_away_time, false)
end

-- 检查当前是否还有弹药
local function isNoAmmo(context)
    return (not context:hasBulletInBarrel()) and (context:getAmmoCount() <= 0)
end

-- 播放检视动画
local function runInspectAnimation(context)
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    if (isNoAmmo(context)) then
        context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

-- 播放换弹动画
local function runReloadAnimation(context)
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    if (isNoAmmo(context)) then
        context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

-- ========== 弹药显示（不空挂状态） ==========
function normal_state.update(this, context)
    if (isNoAmmo(context)) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK))
        context:trigger(this.INPUT_BOLT_CAUGHT)
    else
        local a = context:getAmmoCount()
        if (a < 9) then
            context:setAnimationProgress(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), 0.1 + (7 - a) * 0.5, false)
        else
            context:setAnimationProgress(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), 0.1, false)
        end
    end
end

function normal_state.entry(this, context)
    context:runAnimation("static_ammo_display", context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), false, PLAY_ONCE_STOP, 0)
    this.bolt_caught_states.normal.update(this, context)
end

-- ========== 趴下状态 ==========
local crawl_states = {
    draw = {},
    normal = {},
    crawl = {},
    played_animation = 0
}

function crawl_states.normal.transition(this, context, input)
    if (context:isCrawl()) then
        return this.crawl_states.crawl
    end
end

function crawl_states.crawl.entry(this, context)
    crawl_states.played_animation = 0
end

function crawl_states.crawl.update(this, context)
    -- 主轨播放、滑轨空闲：脚架展开
    if ((not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) and context:isStopped(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        context:runAnimation("crawl_bipod", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.5)
        crawl_states.played_animation = 1
    end
    -- 主轨空闲、滑轨空闲：趴下起手式
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)) and context:isStopped(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        context:runAnimation("crawl_start", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.5)
    end
    -- 主轨空闲、滑轨挂起：循环趴下 / 手臂归位
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)) and context:isHolding(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        if (crawl_states.played_animation == 0) then
            context:runAnimation("crawl", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
        elseif (crawl_states.played_animation == 1) then
            context:runAnimation("crawl_handup", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
            crawl_states.played_animation = 0
        end
    end
    -- 主轨播放、滑轨挂起：手臂归位到默认态
    if ((not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) and context:isHolding(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        if (crawl_states.played_animation == 0) then
            context:runAnimation("crawl_handdown", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
            crawl_states.played_animation = 1
        end
    end
end

function crawl_states.crawl.transition(this, context, input)
    if (not context:isCrawl() or this.main_track_states.final.isfinal == 1) then
        if (not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
            context:runAnimation("crawl_bipod_end", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("crawl_end", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_STOP, 0.2)
        end
        return this.crawl_states.normal
    end
end

-- ========== 主闲置态 ==========
function main_states.transition(this, context, input)
    -- 丢枪
    if (input == INPUT_PUT_AWAY) then
        runPutAwayAnimation(context)
        this.main_track_states.final.isfinal = 1
        return this.main_track_states.final
    end
    -- 换弹
    if (input == INPUT_RELOAD) then
        runReloadAnimation(context)
        -- 标记换弹中，记录起始时间并根据空仓/战术设定不同清零延迟
        belt_state.is_reloading = true
        belt_state.reload_start_time = context:getCurrentTimestamp()
        if (isNoAmmo(context)) then
            belt_state.reload_delay = belt_state.reload_empty_delay
        else
            belt_state.reload_delay = belt_state.reload_tactical_delay
        end
        return this.main_track_states.idle
    end
    -- 开火
    if (input == INPUT_SHOOT) then
        context:popShellFrom(0)
        belt_state.belt_count = math.min(belt_state.belt_count + 1, belt_state.max_count)
        return this.main_track_states.idle
    end
    -- 拉栓
    if (input == INPUT_BOLT) then
        context:runAnimation("bolt", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
    -- 检视
    if (input == INPUT_INSPECT and context:getAimingProgress() < 1) then
        runInspectAnimation(context)
        return this.main_track_states.inspect
    end
    -- 掏枪：清空弹链进度
    if (input == INPUT_DRAW) then
        belt_state.belt_count = 0
        belt_state.is_reloading = false
    end
    -- 近战-配件
    if (input == INPUT_BAYONET_MUZZLE) then
        local counter = this.main_track_states.bayonet_counter
        local animationName = "melee_bayonet_" .. tostring(counter + 1)
        this.main_track_states.bayonet_counter = (counter + 1) % 3
        context:runAnimation(animationName, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
    -- 近战-枪托
    if (input == INPUT_BAYONET_STOCK) then
        context:runAnimation("melee_stock", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
    -- 近战-推击
    if (input == INPUT_BAYONET_PUSH) then
        context:runAnimation("melee_push", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
end

-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    bolt_caught_states = setmetatable({
        normal = normal_state,
    }, {__index = bolt_caught_states}),
    main_track_states = setmetatable({
        idle = main_states
    }, {__index = main_track_states}),
    BELT_TRACK = BELT_TRACK,
    crawl_states = crawl_states,
    belt_state = belt_state
}, {__index = default})

function M:initialize(context)
    default.initialize(self, context)
end

function M:states()
    return {
        self.belt_state,
        self.base_track_state,
        self.bolt_caught_states.normal,
        self.over_heat_states.normal,
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle,
        self.ADS_states.normal,
        self.slide_states.normal,
        self.crawl_states.normal
    }
end

return M
