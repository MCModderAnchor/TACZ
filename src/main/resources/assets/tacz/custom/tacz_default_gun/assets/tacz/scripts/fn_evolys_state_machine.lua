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
local main_states = setmetatable({}, {__index = main_track_states.idle})

local normal_state = setmetatable({}, {__index = bolt_caught_states.normal})

local function isEaster()
    local flag = math.random(1, 1000)
    -- 改下面一行的数字决定概率，概率是千分之 x
    if (flag <= 20) then
        return true
    end
    return false
end

-- 播放丢枪动画的方法
local function runPutAwayAnimation(context)
    local put_away_time = context:getPutAwayTime()
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    -- 播放 put_away 动画,并且将其过渡时长设为从上下文里传入的 put_away_time * 0.75
    context:runAnimation("put_away", track, false, PLAY_ONCE_HOLD, put_away_time * 0.75)
    -- 设定动画进度为最后一帧
    context:setAnimationProgress(track, 1, true)
    -- 将动画进度向前拨动 {put_away_time}
    context:adjustAnimationProgress(track, -put_away_time, false)
end

-- 检查当前是否还有弹药
local function isNoAmmo(context)
    -- 这里同时检查了枪管和弹匣
    return (not context:hasBulletInBarrel()) and (context:getAmmoCount() <= 0)
end

-- 播放检视动画的方法
local function runInspectAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    -- 根据当前整枪内是否还有弹药决定是播放普通检视还是空枪检视
    if (isNoAmmo(context)) then
        context:runAnimation("inspect_empty", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("inspect", track, false, PLAY_ONCE_STOP, 0.2)
    end
end

-- 播放换弹动画的方法
local function runReloadAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    -- 根据当前整枪内是否还有弹药决定是播放战术换弹还是空枪换弹
    if (isNoAmmo(context)) then
        if (isEaster()) then
            context:runAnimation("reload_empty_easter", track, false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
        end
    else
        if (isEaster()) then
            context:runAnimation("reload_tactical_easter", track, false, PLAY_ONCE_STOP, 0.2)
        else
            context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
        end
    end
end

-- 更新"不空挂"状态
function normal_state.update(this, context)
    -- 如果弹药数量是 0 了,那么立刻手动触发一次转到"空挂"状态的输入
    if (isNoAmmo(context)) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK))
        context:trigger(this.INPUT_BOLT_CAUGHT)
    else
        local a = context:getAmmoCount()
        if (a < 9) then
            context:setAnimationProgress(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK),0.1+(8-a)*0.5,false)
        else
            context:setAnimationProgress(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK),0.1,false)
        end
    end
end

local sight_state = {}

function sight_state.update(this, context)
    local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
    if (context:getAttachment("SCOPE") == "tacz:empty") then
        context:runAnimation("sight_on", track, true, PLAY_ONCE_STOP, 0)
    else
        context:runAnimation("sight_off", track, true, PLAY_ONCE_STOP, 0)
    end
end

-- 进入"不空挂"状态
function normal_state.entry(this, context)
    context:runAnimation("static_ammo_display", context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), false, PLAY_ONCE_STOP, 0)
    this.bolt_caught_states.normal.update(this, context)
end

local crawl_states = {
    draw = {},
    normal = {},
    crawl = {},
    played_animation = 0
}

function crawl_states.normal.transition(this, context, input)
    -- 趴下时切到趴下状态
    if (context:isCrawl()) then
        return this.crawl_states.crawl
    end
end

function crawl_states.crawl.entry(this, context)
    -- 重置主轨道动画标志位
    crawl_states.played_animation = 0
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
        --主轨道播放过动画 用单独的手臂归为动画回到趴下状态并重置标志位
        elseif (crawl_states.played_animation == 1) then
            context:runAnimation("crawl_handup", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.4)
            crawl_states.played_animation = 0
        end
    end
    -- 主轨道正在播放动画 且 趴下轨道被挂起 时将趴下的叠加层清除掉并将标志位置1
    if ((not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) and context:isHolding(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        if (crawl_states.played_animation == 0) then
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
        return this.crawl_states.normal
    end
end

-- 转出闲置态
function main_states.transition(this, context, input)
    -- 玩家从枪切到其他物品的时候会自动输入丢枪的信号,不用手动触发,只要检测就好了
    if (input == INPUT_PUT_AWAY) then
        runPutAwayAnimation(context)
        this.main_track_states.final.isfinal = 1
        -- 丢枪后转到最终态
        return this.main_track_states.final
    end
    -- 玩家拿着枪按下 R (或者别的什么自己绑定的换弹键)时会自动输入换弹信号
    if (input == INPUT_RELOAD) then
        runReloadAnimation(context)
        -- 换弹动画播放完后返回闲置态(也就是返回自己)
        return this.main_track_states.idle
    end
    -- 玩家在射击时会自动输入 shoot 信号
    if (input == INPUT_SHOOT) then
        context:popShellFrom(0) -- 默认射击抛壳
        -- 返回闲置态(也就是返回自己),这里不播放射击动画是因为射击动画应该在 gun_kick 状态里播
        return this.main_track_states.idle
    end
    -- 玩家在使用栓动武器射击完成后拉栓会自动输入 bolt 信号
    if (input == INPUT_BOLT) then
        context:runAnimation("bolt", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        -- 拉栓动画播放完后返回闲置态
        return this.main_track_states.idle
    end
    -- 玩家按下检视键后会输入检视信号
    if (input == INPUT_INSPECT and context:getAimingProgress() < 1) then
        runInspectAnimation(context)
        -- 检视需要转到检视态,因为检视过程中屏幕中央准星是隐藏的,因此需要一个检视态来调控准星
        return this.main_track_states.inspect
    end
    -- 玩家使用近战武器时输入的近战信号,分为近战配件、枪托、推击这三种情况
    -- 近战配件可以使用多种近战动画,而枪托和推击则是在枪配置文件里写的"无近战配件时的近战攻击",只能使用一个动画
    if (input == INPUT_BAYONET_MUZZLE) then
        -- 这里是一个顺序播放动画的方法,通过存储在状态里的 counter 决定当前播放的是第几个近战动画, animationName 是一个组合起来的字符串
        -- 这样写法会使依次运行 "melee_bayonet_1" "melee_bayonet_2" "melee_bayonet_3" 这三个动画, 3 运行完后再近战则会返回 1
        local counter = this.main_track_states.bayonet_counter
        local animationName = "melee_bayonet_" .. tostring(counter + 1)
        this.main_track_states.bayonet_counter = (counter + 1) % 3
        context:runAnimation(animationName, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
    -- 枪托肘完之后返回闲置态
    if (input == INPUT_BAYONET_STOCK) then
        context:runAnimation("melee_stock", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0.2)
        return this.main_track_states.idle
    end
    -- 推击完之后返回闲置态
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
    crawl_states = crawl_states,
    sight_state = sight_state
}, {__index = default})
function M:initialize(context)
    default.initialize(self, context)
end
-- 继承默认状态机需要重新初始化状态
function M:states()
    return {
        self.sight_state,
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
-- 导出状态机
return M