-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
--
-- 本枪是"继承型"状态机，只保留与默认状态机不同的部分，其余全部透传给 tacz_default_state_machine。
--
-- 本枪的特化集中在"转管机枪"上：
--   1. 枪管持续旋转，由 over_heat_states 与 spin_states 两个子状态机共同驱动
--      常态旋转走 SPIN_NORMAL_TRACK，开火加速走 SPIN_FIRE_TRACK
--   2. 过热与旋转联动：过热时先停转、播过热动画，冷却后再恢复旋转
--   3. 过热期间禁止检视；收枪 / 检视时把旋转停下（INPUT_STOP_SPIN）
--
-- 轨道常量一律取自默认状态机（不要沿用本枪旧版那份独立编号，否则会和继承来的
-- 默认实现按两套编号操作同一批轨道，导致错位）。只有两条 spin 轨道是新的，
-- 用 increment(default.blending_track_top) 续接在默认分配之后（落在 10、11）。
local default = require("tacz_default_state_machine_v2")

local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local BLENDING_TRACK_LINE = default.BLENDING_TRACK_LINE
local MAIN_TRACK = default.MAIN_TRACK
local OVER_HEAT_TRACK = default.OVER_HEAT_TRACK

local main_track_states = default.main_track_states

-- 栈顶指针自增函数，用于分配新的轨道。default 没有导出 increment，脚本内需要自己定义一份
local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end

-- 两条新增的混合轨道，续接在默认分配之后
local blending_track_top = default.blending_track_top
local SPIN_NORMAL_TRACK = increment(blending_track_top)
local SPIN_FIRE_TRACK = increment(blending_track_top)

-- 检查当前是否处于过热状态
local function isOverHeat(context)
    return context:isOverHeat()
end

-- ===================================================================================== ↓ 主轨道 ↓ ============
local idle_state = setmetatable({}, {__index = main_track_states.idle})

function idle_state.transition(this, context, input)
    -- 收枪时把枪管旋转停下。先发信号，再交给默认实现处理丢枪动画与 final 状态
    if (input == INPUT_PUT_AWAY) then
        context:trigger(this.INPUT_STOP_SPIN)
        return main_track_states.idle.transition(this, context, input)
    end
    -- 过热期间禁止检视
    if (input == INPUT_INSPECT and isOverHeat(context)) then
        return nil
    end
    -- 换弹、射击、拉栓、检视、近战全部交回默认实现
    return main_track_states.idle.transition(this, context, input)
end

local inspect_state = setmetatable({}, {__index = main_track_states.inspect})

-- 进入检视态：先把旋转停下，再走默认实现（隐藏屏幕中央准星）
function inspect_state.entry(this, context)
    context:trigger(this.INPUT_STOP_SPIN)
    main_track_states.inspect.entry(this, context)
end

-- ===================================================================================== ↓ 过热 & 旋转 ↓ ============
-- 过热部分与旋转部分是一套联动机制：
--   static  —— 刚掏完枪，等主轨道空闲后进入 normal
--   normal  —— 常态，枪管在 SPIN_NORMAL_TRACK 上循环转
--   over_heat —— 过热，停转 + 播过热动画，冷却后再恢复旋转
-- 与默认状态机的同名状态相比，这里的 normal / over_heat 都揉进了枪管旋转的处理。

local over_heat_static_state = {}

function over_heat_static_state.transition(this, context, input)
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        return this.over_heat_states.normal
    end
end

local over_heat_normal_state = setmetatable({}, {__index = default.over_heat_states.normal})

-- 进入"不过热"状态：枪管开始持续旋转
-- 主轨道还忙着（玩家正在检视、或刚收枪）时先不起转，否则会把 STOP_SPIN 的效果冲掉；
-- 等主轨道空闲、spin_states 回到 normal 时，由 spin_states.normal.entry 重新起转
function over_heat_normal_state.entry(this, context)
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:runAnimation("spin", context:getTrack(BLENDING_TRACK_LINE, SPIN_NORMAL_TRACK), true, PLAY_ONCE_HOLD, 0)
    end
end

-- 更新"不过热"状态
function over_heat_normal_state.update(this, context)
    local track = context:getTrack(BLENDING_TRACK_LINE, SPIN_NORMAL_TRACK)
    if (context:isHolding(track)) then
        if (isOverHeat(context)) then
            context:trigger(this.INPUT_OVER_HEAT)
        else
            -- 轨道挂起了就接着转，保证枪管一直在转
            context:runAnimation("spin", track, true, PLAY_ONCE_HOLD, 0)
        end
    end
end

-- 转出"不过热"状态
function over_heat_normal_state.transition(this, context, input)
    -- 如果收到了"过热"的输入,那么直接转到"过热"状态
    if (input == this.INPUT_OVER_HEAT) then
        return this.over_heat_states.over_heat
    end
    -- 收枪 / 检视时停转
    if (input == this.INPUT_STOP_SPIN) then
        context:stopAnimation(context:getTrack(BLENDING_TRACK_LINE, SPIN_NORMAL_TRACK))
        return this.over_heat_states.static
    end
end

local over_heat_over_state = setmetatable({}, {__index = default.over_heat_states.over_heat})

-- 更新"过热"状态：等过热的一次性动画播完后判冷却、并维持枪管旋转
function over_heat_over_state.update(this, context)
    -- 先等过热的一次性动画播完
    if (not context:isStopped(context:getTrack(BLENDING_TRACK_LINE, OVER_HEAT_TRACK))) then
        return
    end
    -- 冷却判定绝对不能挂在"枪管轨道是否挂起"上：收枪 / 检视的 INPUT_STOP_SPIN 会把枪管
    -- 停掉，而本状态的 transition 不看这个输入，于是永远进不了这里、永远触发不了
    -- COOLING_HEAT，状态机就卡死在"过热"里。表现是下次过热时状态没有变化，
    -- entry 不会重跑，过热动画就不播了
    if (not isOverHeat(context)) then
        context:trigger(this.INPUT_COOLING_HEAT)
        return
    end
    -- 仍在过热：枪管继续转
    local spinTrack = context:getTrack(BLENDING_TRACK_LINE, SPIN_NORMAL_TRACK)
    if (context:isHolding(spinTrack)) then
        context:runAnimation("spin", spinTrack, true, PLAY_ONCE_HOLD, 0)
    end
end

local over_heat_states = setmetatable({
    static = over_heat_static_state,
    normal = over_heat_normal_state,
    over_heat = over_heat_over_state
}, {__index = default.over_heat_states})

-- 旋转部分：控制开火时的枪管加速旋转
--   static —— 刚掏完枪，等主轨道空闲后进入 normal
--   normal —— 跟随 over_heat_states.normal 的常态旋转，收到射击输入后转 fire
--   fire   —— 在 SPIN_FIRE_TRACK 上叠加播放旋转，停火后转回 normal

local spin_states = {
    static = {},
    normal = {},
    fire = {}
}

function spin_states.static.transition(this, context, input)
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        return this.spin_states.normal
    end
end

-- 进入"默认旋转"状态：把枪管转起来。
-- 本状态机在收到 INPUT_STOP_SPIN 时会停转并回到 static，等主轨道空闲后转回这里——
-- 所以"收枪 / 检视结束后重新起转"是由这里负责的（over_heat_states 那边在检视期间
-- 不会起转，正是为了让 STOP_SPIN 的效果保持住）
function spin_states.normal.entry(this, context)
    context:runAnimation("spin", context:getTrack(BLENDING_TRACK_LINE, SPIN_NORMAL_TRACK), true, PLAY_ONCE_HOLD, 0)
end

-- 转出"默认旋转"状态
function spin_states.normal.transition(this, context, input)
    -- 如果收到了"开火"的输入,那么直接转到"开火旋转"状态
    if (input == INPUT_SHOOT) then
        return this.spin_states.fire
    end
    if (input == this.INPUT_STOP_SPIN) then
        context:stopAnimation(context:getTrack(BLENDING_TRACK_LINE, SPIN_NORMAL_TRACK))
        return this.spin_states.static
    end
end

-- 进入开火状态
function spin_states.fire.entry(this, context)
    -- 首先播放一次动画并将轨道挂起
    context:runAnimation("spin", context:getTrack(BLENDING_TRACK_LINE, SPIN_FIRE_TRACK), true, PLAY_ONCE_HOLD, 0.2)
end

-- 开火过程中实时更新
function spin_states.fire.update(this, context)
    local track = context:getTrack(BLENDING_TRACK_LINE, SPIN_FIRE_TRACK)
    local lastShootTime = context:getLastShootTimestamp()
    local nowTime = context:getCurrentTimestamp()
    local shootInterval = context:getShootInterval()
    -- 判断轨道是否被挂起，判断挂起是为了防止叠加的旋转动画播到一半停止导致的枪管角度瞬移
    if (context:isHolding(track)) then
        -- 判断枪有没有连射，给 2 tick 的容错以防游戏卡顿
        if (nowTime - lastShootTime > shootInterval + 100) then
            -- 轨道被挂起 且 没有连射 就触发停止射击的输入
            context:trigger(this.INPUT_STOP_SHOOT)
        else
            -- 轨道被挂起 且 在连射 就继续叠加播放旋转动画
            context:runAnimation("spin", track, true, PLAY_ONCE_HOLD, 0.2)
        end
    end
end

-- 转出射击态
function spin_states.fire.transition(this, context, input)
    if (input == this.INPUT_STOP_SHOOT) then
        -- 收到停止射击信号则播放枪管转速逐渐下降的动画
        context:runAnimation("spin_low", context:getTrack(BLENDING_TRACK_LINE, SPIN_FIRE_TRACK), true, PLAY_ONCE_STOP, 0.2)
        return this.spin_states.normal
    end
end

local M = setmetatable({
    main_track_states = setmetatable({
        -- 覆写：收枪/检视时停转、过热时禁止检视
        idle = idle_state,
        inspect = inspect_state,
        -- 下面三个是为了隔离父级的共享表，避免跨枪串味：
        -- default.initialize 会无条件写 charge.can_charge，不建自己的表就会原地写父表
        charge = {},
        -- isfinal 会被滑铲门控读取（多个枪械脚本里都有 not isCrawl() or ...isfinal == 1 的判断），
        -- 写进共享表会影响别的枪
        final = { isfinal = -1 },
        allow_run_overlay = false
    }, {__index = main_track_states}),
    -- 过热与旋转：本枪的核心特化
    over_heat_states = over_heat_states,
    spin_states = spin_states,
    SPIN_NORMAL_TRACK = SPIN_NORMAL_TRACK,
    SPIN_FIRE_TRACK = SPIN_FIRE_TRACK,
    -- 输入
    INPUT_STOP_SHOOT = "input_stop_shoot",
    INPUT_STOP_SPIN = "stop_spin"
}, {__index = default})

-- 状态机初始化函数，在切枪的时候调用
-- default.initialize 会按 sharing 的栈顶指针备足轨道，此时 blending 计数已被上面两次
-- increment 推到 12，所以 10、11 两条 spin 轨道一定会被备足
function M:initialize(context)
    default.initialize(self, context)
end

function M:states()
    -- over_heat 与 spin 都必须从 static 起手：它们在等主轨道（掏枪）播完才转 normal，
    -- 也就是"枪管在掏枪动作结束后才开始转"。
    -- 本枪不列出 slide_states.normal（机枪不能滑铲，数据里 can_crawl 也是 false）
    return {
        self.base_track_state,
        self.bolt_caught_states.normal,
        self.over_heat_states.static,
        self.spin_states.static,
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle,
        self.ADS_states.normal
    }
end

return M
