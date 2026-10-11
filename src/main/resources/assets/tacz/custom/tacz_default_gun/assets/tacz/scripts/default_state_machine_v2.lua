-- 这些栈顶指针在分配新的轨道行和轨道时起作用
-- 轨道行 栈顶指针
local track_line_top = {value = 0}
-- 主轨道行 的 轨道 栈顶指针
local static_track_top = {value = 0}
-- 混合轨道行 的 轨道 栈顶指针
local blending_track_top = {value = 0}
-- 栈顶指针自增函数，用于分配新的轨道行或者轨道
local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end

-- 主轨道行 和 其中的轨道
local STATIC_TRACK_LINE = increment(track_line_top)

-- 五条低轨道，会被主轨道行中的其他高级轨道覆盖
local PRE_PARALLEL_TRACK_1 = increment(static_track_top)
local PRE_PARALLEL_TRACK_2 = increment(static_track_top)
local PRE_PARALLEL_TRACK_3 = increment(static_track_top)
local PRE_PARALLEL_TRACK_4 = increment(static_track_top)
local PRE_PARALLEL_TRACK_5 = increment(static_track_top)

-- 主轨道行上的高级轨道
local BASE_TRACK = increment(static_track_top)
local BOLT_CAUGHT_TRACK = increment(static_track_top)
local SAFETY_TRACK = increment(static_track_top) -- 待实现
local ADS_TRACK = increment(static_track_top)
local MAIN_TRACK = increment(static_track_top)
local SPRINT_TRACK = increment(static_track_top)

-- 五条顶级轨道，会覆盖主轨道行中的其他轨道
local PARALLEL_TRACK_1 = increment(static_track_top)
local PARALLEL_TRACK_2 = increment(static_track_top)
local PARALLEL_TRACK_3 = increment(static_track_top)
local PARALLEL_TRACK_4 = increment(static_track_top)
local PARALLEL_TRACK_5 = increment(static_track_top)

-- 开火的轨道行
local GUN_KICK_TRACK_LINE = increment(track_line_top)

-- 混合轨道行 和 其中的轨道，用于叠加动画，如跑步走路跳跃过热
local BLENDING_TRACK_LINE = increment(track_line_top)

local MOVEMENT_TRACK = increment(blending_track_top)
local SLIDE_TRACK = increment(blending_track_top)
local OVER_HEAT_TRACK = increment(blending_track_top)
local OVER_HEATING_TRACK = increment(blending_track_top)
local LOOP_TRACK = increment(blending_track_top)

-- 五条混合轨道，应叠加到其他轨道上
local BLEND_TRACK_1 = increment(blending_track_top)
local BLEND_TRACK_2 = increment(blending_track_top)
local BLEND_TRACK_3 = increment(blending_track_top)
local BLEND_TRACK_4 = increment(blending_track_top)
local BLEND_TRACK_5 = increment(blending_track_top)

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

-- 播放换弹动画的方法
local function runReloadAnimation(context)
    -- 此处获取的轨道是位于主轨道行上的主轨道
    local track = context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)
    -- 根据当前整枪内是否还有弹药决定是播放战术换弹还是空枪换弹
    if (isNoAmmo(context)) then
        context:runAnimation("reload_empty", track, false, PLAY_ONCE_STOP, 0.2)
    else
        context:runAnimation("reload_tactical", track, false, PLAY_ONCE_STOP, 0.2)
    end
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

-- 检查当前是否处于过热状态
local function isOverHeat(context)
    return context:isOverHeat()
end

-- 基础轨道上的状态,这个状态用于循环播放 static_idle 动画。
local base_track_state = {}

-- 进入基础状态,直接播放 static_idle
function base_track_state.entry(this, context)
    -- 在 主轨道行 的 基础轨道 上循环播放 static_idle
    context:runAnimation("static_idle", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, LOOP, 0)
    context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK))
end

-- 空挂部分,该部分到 153 行结束
-- 由于空挂分为"空挂"和"不空挂"两类,因此这里面需要两个状态来调控当前武器
-- 空挂的两个状态之间是会来回切换的,因此每个子状态都需要以下三个方法来操作
-- entry 方法是进入该状态时发生的事,只在进入状态时执行一次
-- update 方法是在该状态时每一渲染帧都会调用的事,注意这里是每一渲染帧,并不是游戏的 tick ,因此这个方法的执行频率远远大于 tick (除非你电脑玩游戏连 20 帧都不到)
-- transition 方法是该状态的转出,只在转换为别的状态时执行一次
--
-- 这部分的一般实现逻辑是
-- entry 负责在进入 update 前播放相关动画并进入 update
-- update 负责实时检测是否需要转出状态,如有需要则触发 transition
-- transition 负责接收 update 传来的信息,在被触发后需要停止相关动画并转去其他状态
-- 这套逻辑在之后的主状态里一样生效
local bolt_caught_states = {
    -- normal 是不空挂的正常状态
    normal = {},
    -- bolt_caught 是空挂时的状态
    bolt_caught = {}
}

-- 更新"不空挂"状态
function bolt_caught_states.normal.update(this, context)
    -- 如果弹药数量是 0 了,那么立刻手动触发一次转到"空挂"状态的输入
    if (isNoAmmo(context)) then
        context:trigger(this.INPUT_BOLT_CAUGHT)
    end
end

-- 进入"不空挂"状态
function bolt_caught_states.normal.entry(this, context)
    -- 因为进入不空挂状态没什么需要做的,因此什么都不做直接转进该状态
    this.bolt_caught_states.normal.update(this, context)
end

-- 转出"不空挂"状态
function bolt_caught_states.normal.transition(this, context, input)
    -- 如果收到了"空挂"的输入,那么直接转到"空挂"状态,"'空挂'的输入"是在上文 update 方法中出现的
    if (input == this.INPUT_BOLT_CAUGHT) then
        return this.bolt_caught_states.bolt_caught
    end
end

-- 进入"空挂"状态
function bolt_caught_states.bolt_caught.entry(this, context)
    -- 进入空挂时在主轨道行的空挂轨道播放空挂的动画
    context:runAnimation("static_bolt_caught", context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), false, LOOP, 0)
end

-- 更新"空挂"状态
function bolt_caught_states.bolt_caught.update(this, context)
    -- 如果检测到子弹数不为 0 了(此时是换弹了),那么手动触发一次转到"不空挂"状态的输入
    if (not isNoAmmo(context)) then
        context:trigger(this.INPUT_BOLT_NORMAL)
    end
end

-- 转出"空挂"状态
function bolt_caught_states.bolt_caught.transition(this, context, input)
    -- 如果收到了来自上文 update 方法的输入,则转到"不空挂"状态
    if (input == this.INPUT_BOLT_NORMAL) then
        -- 由于并没有一个"不空挂"的动画,因此必须在这里把空挂动画停止了才能转到"不空挂"状态,否则你会在换完弹之后发现依旧处于空挂状态
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK))
        return this.bolt_caught_states.normal
    end
end
-- 结束空挂部分

-- 主轨道状态,该部分到 271 行结束
-- 主轨道控制的是武器的基本动作,包括换弹,检视,刺刀攻击,掏枪,丢枪
-- 除了检视，其他动作不需要单独的状态控制。
-- 检视状态需要被射击输入打断，此外，进入检视时，需要隐藏准心，退出检视时恢复准心。
-- 除了上文的那三个方法，检视还需要 exit 方法，用于恢复准心渲染
local main_track_states = {
    -- 主轨道当前这个动画是否允许与 run 叠加。
    --
    -- 主轨道动画分两类，区别在于它自带的关键帧会不会和 run 的 root 旋转打架：
    --   允许叠加：掏枪、换弹、拉栓。它们没有会冲突的 root 旋转；而且换弹的功能时长
    --     (战术 2.0s / 空仓 2.6s)比动画(2.4s / 3.05s)短，玩家恢复奔跑时动画还剩一截尾巴，
    --     让 run 当场接上、与尾巴叠着播，动画才不会比 FOV 慢半拍
    --     (冲刺在 FEEDING 阶段被禁、一进 FINISHING 就解禁，FOV 当场就变，
    --      动画却要等主轨道播完才跟，观感就是慢半拍)。
    --   不允许叠加：检视、近战。它们自带 root 旋转，和 run 叠在一起枪械会拧得异常，
    --     只能降级到 walk 去和 walk 叠加。
    --
    -- 置位：updateRunOverlay 收到掏枪/换弹/拉栓输入时（start.transition 里还有一份冗余的
    --     掏枪置位，理由见那里的注释）。
    -- 清除：updateRunOverlay 收到上表"不允许"的那几个输入时；以及下面的 idle.update 在
    --     主轨道动画播完时（这一处是兜底，有脚本重写 idle.update 而不调父级，所以不能只靠它）。
    allow_run_overlay = false,
    -- 起始
    start = {},
    -- 闲置,当玩家把枪拿在手里站定并什么也不做的时候就是这种情况
    idle = {},
    -- 检视
    inspect = {},
    -- 结束
    final = {
        isfinal = -1
    },
    -- 刺刀攻击的计数器
    bayonet_counter = 0,
    -- 延迟扳机
    charge = {}
}

-- 转出 start (其实就是掏枪)
function main_track_states.start.transition(this, context, input)
    -- 玩家手里拿到枪的那一瞬间会自动输入一个 draw 的信号,不用手动触发
    if (input == INPUT_DRAW) then
        -- 收到 draw 信号后在主轨道行的主轨道上播放掏枪动画,然后转到闲置态
        this.main_track_states.final.isfinal = -1
        context:runAnimation("draw", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0)
        -- 掏枪允许与 run 叠加：玩家常常是边跑边切枪，此时不该把移动轨道降级成行走，
        -- 而应让 run(含 run_start) 和 draw 在各自的轨道上叠加。
        -- 掏枪结束时移动轨道早已在跑循环段，不需要任何转换，自然也就不会重播起手式或硬切。
        --
        -- 这里和 updateRunOverlay 的 INPUT_DRAW 分支是双保险：两边都设同一个值，不会冲突。
        -- 之所以留两份，是因为枪械脚本覆盖状态函数防不胜防——重写 start_state.transition
        -- 的脚本(如 spas_12)绕过这里，重写 movement 的脚本绕过那边，任一边存活即可生效。
        this.main_track_states.allow_run_overlay = true
        return this.main_track_states.idle
    end
end

-- 更新闲置态
function main_track_states.idle.update(this, context)
    -- 主轨道动画播完后轨道恢复空闲，叠加许可随之失效。
    -- 必须等动画真正停止才清，否则移动轨道会在动画还没播完时就被降级成行走。
    if (this.main_track_states.allow_run_overlay
        and context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        this.main_track_states.allow_run_overlay = false
    end
end

-- 转出闲置态
function main_track_states.idle.transition(this, context, input)
    -- 注意：叠加许可的同步不在这里，而在 movement 那边的 updateRunOverlay。
    -- 原因是各枪械脚本重写了 idle_state.transition 并会提前 return，这个函数常被绕过。
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

-- 进入检视态
function main_track_states.inspect.entry(this, context)
    -- 检视是需要隐藏屏幕中央准星
    context:setShouldHideCrossHair(true)
end

-- 退出检视态
function main_track_states.inspect.exit(this, context)
    context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
    -- 退出后恢复屏幕中央准星
    context:setShouldHideCrossHair(false)
end

-- 更新检视态
function main_track_states.inspect.update(this, context)
    -- 当检测到动画停止了(播完了)时手动触发一次退出信号
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:trigger(this.INPUT_INSPECT_RETREAT)
    end
    if (context:isCharging()) then
        context:trigger(this.INPUT_INSPECT_BREAK_OUT)
    end
end

-- 转出检视态
function main_track_states.inspect.transition(this, context, input)
    -- 当收到来自 update 的退出信号时返回到闲置态,此时不需要停止动画是因为在 update 里是动画已经停止了才发出的退出信号
    if (input == this.INPUT_INSPECT_RETREAT) then
        return this.main_track_states.idle
    end
    -- 当收到来自 update 的打断信号时返回到闲置态并停止动画
    if (input == this.INPUT_INSPECT_BREAK_OUT) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
        return this.main_track_states.idle
    end
    -- 特殊地,射击与瞄准应当打断检视,当检测到射击输入或瞄准进度不为0时应该直接停止动画并返回闲置态
    if (input == INPUT_SHOOT or context:getAimingProgress() > 0) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
        return this.main_track_states.idle
    end
    return this.main_track_states.idle.transition(this, context, input)
end
-- 结束主状态部分

-- 射击态,没什么需要调控的
local gun_kick_state = {}

function gun_kick_state.transition(this, context, input)
    -- 玩家按下开火键时需要在射击轨道行里寻找空闲轨道去播放射击动画(如果没有空闲会分配新的),需要注意的是射击动画要向下混合
    if (input == INPUT_SHOOT) then
        local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
        -- 这里是混合动画，一般是可叠加的 gun kick
        context:runAnimation("shoot", track, true, PLAY_ONCE_STOP, 0)
    end
    return nil
end

-- 移动轨道的状态,这部分到 450 行结束
-- 同步主轨道动画的"允许与 run 叠加"许可(allow_run_overlay)。
--
-- 主轨道会被一批输入抢占，收到这些输入时就该重新判定新动画属于哪一类：
--   掏枪 / 换弹 / 拉栓 —— 允许叠加，理由见 main_track_states.allow_run_overlay 的注释
--   检视 / 近战 —— 不允许，必须降级到 walk
--
-- 为什么判定写在这里，而不是 main_track_states 那边：
-- 各枪械脚本普遍重写了自己的 main_track_states 状态，并在自行处理完后直接 return，根本
-- 不会走到默认实现。已经踩过两次：
--   ak47 等重写 idle_state.transition，自行处理换弹/检视/丢枪后 return
--   spas_12 重写 start_state.transition，自行播放掏枪动画后 return
-- 而 movement_track_states 没有被任何枪械脚本重写过（只有 hk_mk23 / minigun 这类整份
-- 复制的状态机自带一套），且状态机 trigger 时会把同一个输入广播给所有轨道行的当前状态，
-- 所以这里是唯一稳定能收到这些输入的地方。
local function updateRunOverlay(this, input)
    if (input == INPUT_DRAW or input == INPUT_RELOAD or input == INPUT_BOLT) then
        this.main_track_states.allow_run_overlay = true
    elseif (input == INPUT_PUT_AWAY or input == INPUT_INSPECT
        or input == INPUT_BAYONET_MUZZLE or input == INPUT_BAYONET_STOCK
        or input == INPUT_BAYONET_PUSH) then
        this.main_track_states.allow_run_overlay = false
    end
end

local movement_track_states = {
    -- 静止不动(或者在天上)
    idle = {},
    -- 奔跑, -1 是没有奔跑, 0 是在奔跑中
    run = {
        mode = -1,
        time = 0
    },
    -- 行走, -1 是没有行走, 0 是在空中, 1 是正在瞄准, 2 是在向前走, 3 是向后退, 4 是向侧面走
    walk = {
        mode = -1
    },
    -- 战术冲刺
    sprint = {
        mode = -1
    }
}

-- 更新静止态
function movement_track_states.idle.update(this, context)
    -- 此处获取的是混合轨道行的移动轨道
    local track = context:getTrack(BLENDING_TRACK_LINE, MOVEMENT_TRACK)
    -- 如果轨道空闲，则播放 idle 动画
    -- 注意此处没有写成是在 entry 播放 idle 动画是因为要实时检测轨道是否空闲
    if (context:isStopped(track) or context:isHolding(track)) then
        context:runAnimation("idle", track, true, LOOP, 0)
    end
end

-- 转出静止态
function movement_track_states.idle.transition(this, context, input)
    -- 先同步主轨道动画的叠加许可，再判断降级
    updateRunOverlay(this, input)
    -- 如果玩家在奔跑则转去奔跑态
    if (input == INPUT_RUN) then
        -- 主轨道允许叠加时视同空闲：玩家本来就在跑，让 run 正常起手，
        -- 与主轨道上的掏枪/换弹尾巴叠加播放，而不是降级成行走。
        if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
            or this.main_track_states.allow_run_overlay) then
            return this.movement_track_states.run
        else
            -- 玩家确实在跑，只是主轨道正忙且不允许叠加(检视/近战等)而被降级为行走。
            -- 主轨道空出来后重新进入 run 会正常播 run_start 起手式，这是预期行为
            return this.movement_track_states.walk
        end
    -- 如果玩家在行走则转去行走态
    elseif (input == INPUT_WALK) then
        return this.movement_track_states.walk
    end
end

-- 进入奔跑态
function movement_track_states.run.entry(this, context)
    this.movement_track_states.run.mode = -1
    this.movement_track_states.run.time = context:getCurrentTimestamp()
    -- 此处播放的轨道是混合轨道行的移动轨道,播放的动画是奔跑的起手式,播放结束后是挂起动画而不是停止
    context:runAnimation("run_start", context:getTrack(BLENDING_TRACK_LINE, MOVEMENT_TRACK), true, PLAY_ONCE_HOLD, 0.2)
end

-- 退出奔跑态
function movement_track_states.run.exit(this, context)
    -- 此时播放的动画是奔跑结束回到 idle 的动画,同理播放完后挂起
    context:runAnimation("run_end", context:getTrack(BLENDING_TRACK_LINE, MOVEMENT_TRACK), true, PLAY_ONCE_HOLD, 0.3)
end

-- 更新奔跑态
function movement_track_states.run.update(this, context)
    local track = context:getTrack(BLENDING_TRACK_LINE, MOVEMENT_TRACK)
    local state = this.movement_track_states.run;
    -- 等待 run_start 结束,然后循环播放 run ,此处的判断准则是轨道是否挂起,也就是为什么 entry 里播放动画要选 PLAY_ONCE_HOLD 模式
    if (context:isHolding(track)) then
        context:runAnimation("run", track, true, LOOP, 0.2)
        -- 检测是否奔跑的标志位 0
        state.mode = 0
        context:anchorWalkDist() -- 打 walkDist 锚点，确保 run 动画的起点一致
    end
    if (state.mode ~= -1) then
        if (not context:isOnGround()) then
            -- 如果玩家在空中，则播放 run_hold 动画以稳定枪身
            if (state.mode ~= 1) then
                state.mode = 1
                context:runAnimation("run_hold", track, true, LOOP, 0.6)
            end
        else
            -- 如果玩家在地面，则切换回 run 动画
            if (state.mode ~= 0) then
                state.mode = 0
                context:runAnimation("run", track, true, LOOP, 0.2)
            end
            -- 根据 walkDist 设置 run 动画的进度
            context:setAnimationProgress(track, (context:getWalkDist() % 2.0) / 2.0, true)
        end
    end
end

-- 转出奔跑态
function movement_track_states.run.transition(this, context, input)
    -- 先同步主轨道动画的叠加许可，再判断降级
    updateRunOverlay(this, input)
    -- 收到闲置输入则转去闲置态
    if (input == INPUT_IDLE) then
        return this.movement_track_states.idle
    -- 收到行走输入则转去行走态
    -- 主轨道忙碌时通常要降级为行走，但允许叠加的动画(掏枪/换弹)例外：run 就该和它叠着播
    elseif (input == INPUT_WALK or (not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
        and not this.main_track_states.allow_run_overlay)) then
        return this.movement_track_states.walk
    end
end


-- 进入行走态
function movement_track_states.walk.entry(this, context)
    -- 此时给标志位置为 -1 相当于一个初始化
    this.movement_track_states.walk.mode = -1
end

-- 退出行走态
function movement_track_states.walk.exit(this, context)
    -- 手动播放一次 idle 动画以打断 walk 动画的循环
    context:runAnimation("idle", context:getTrack(BLENDING_TRACK_LINE, MOVEMENT_TRACK), true, PLAY_ONCE_HOLD, 0.4)
end

-- 更新行走态
function movement_track_states.walk.update(this, context)
    -- 此处获取的是混合轨道行的移动轨道
    local track = context:getTrack(BLENDING_TRACK_LINE, MOVEMENT_TRACK)
    -- 这里的 state 代指自身,相当于一个简化写法
    local state = this.movement_track_states.walk
    if (context:getShootCoolDown() > 0) then
        -- 如果刚刚开火，则播放 idle 动画以稳定枪身
        if (state.mode ~= 0) then
            state.mode = 0
            context:runAnimation("idle", track, true, LOOP, 0.3)
        end
    elseif (not context:isOnGround()) then
        -- 如果玩家在空中，则播放 idle 动画以稳定枪身
        if (state.mode ~= 0) then
            state.mode = 0
            context:runAnimation("idle", track, true, LOOP, 0.6)
        end
    elseif (context:getAimingProgress() > 0.5) then
        -- 如果正在喵准，则需要播放 walk_aiming 动画
        if (state.mode ~= 1) then
            state.mode = 1
            context:runAnimation("walk_aiming", track, true, LOOP, 0.3)
        end
    elseif (context:isInputUp()) then
        -- 如果正在向前走，则需要播放 walk_forward 动画
        if (state.mode ~= 2) then
            state.mode = 2
            context:runAnimation("walk_forward", track, true, LOOP, 0.4)
            context:anchorWalkDist() -- 打 walkDist 锚点，确保行走动画的起点一致
        end
    elseif (context:isInputDown()) then
        -- 如果正在向后退，则需要播放 walk_backward 动画
        if (state.mode ~= 3) then
            state.mode = 3
            context:runAnimation("walk_backward", track, true, LOOP, 0.4)
            context:anchorWalkDist() -- 打 walkDist 锚点，确保行走动画的起点一致
        end
    elseif (context:isInputLeft() or context:isInputRight()) then
        -- 如果正在向侧面，则需要播放 walk_sideway 动画
        if (state.mode ~= 4) then
            state.mode = 4
            context:runAnimation("walk_sideway", track, true, LOOP, 0.4)
            context:anchorWalkDist() -- 打 walkDist 锚点，确保行走动画的起点一致
        end
    end
    -- 根据 walkDist 设置行走动画的进度
    if (state.mode >= 1 and state.mode <= 4) then
        context:setAnimationProgress(track, (context:getWalkDist() % 2.0) / 2.0, true)
    end
end

-- 转出行走态,这部分和转出奔跑态是一样的
function movement_track_states.walk.transition(this, context, input)
    -- 先同步主轨道动画的叠加许可，再判断降级
    updateRunOverlay(this, input)
    -- 收到闲置信号则转到闲置态
    if (input == INPUT_IDLE) then
        return this.movement_track_states.idle
    -- 收到奔跑信号则转到奔跑态，允许叠加的动画期间同样放行(理由见 idle.transition)
    elseif (input == INPUT_RUN) then
        if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
            or this.main_track_states.allow_run_overlay) then
            return this.movement_track_states.run
        end
    end
end
-- 结束移动轨道的状态

local slide_states = {
    normal = {},
    slide = {}
}

function slide_states.normal.transition(this, context, input)
    if(context:shouldSlide() and context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)) and not context:isAiming()) then
        return this.slide_states.slide
    end
end

function slide_states.slide.entry(this, context)
    context:runAnimation("slide", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.5)
end

function slide_states.slide.update(this, context)
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)) or context:isHolding(context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK))) then
        context:runAnimation("slide_idle", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, LOOP, 0.4)
    end
end

function slide_states.slide.transition(this, context, input)
    if(not context:shouldSlide() or not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK)) or context:isAiming()) then
        return this.slide_states.normal
    end
end

function slide_states.slide.exit(this, context)
    context:runAnimation("slide_back", context:getTrack(BLENDING_TRACK_LINE, SLIDE_TRACK), true, PLAY_ONCE_HOLD, 0.2)
end

-- 过热部分,该部分到 505 行结束
-- 过热部分的内容完全参照空挂部分

local over_heat_states = {
    -- normal 是不过热的正常状态
    normal = {},
    -- over_heat 是过热时的状态
    over_heat = {}
}

-- 进入"不过热"状态
function over_heat_states.normal.entry(this, context)
    this.over_heat_states.normal.update(this, context)
end

-- 更新"不过热"状态
function over_heat_states.normal.update(this, context)
    if (isOverHeat(context)) then
        context:trigger(this.INPUT_OVER_HEAT)
    end
end

-- 转出"不过热"状态
function over_heat_states.normal.transition(this, context, input)
    if (input == this.INPUT_OVER_HEAT) then
        return this.over_heat_states.over_heat
    end
end

-- 进入"过热"状态
function over_heat_states.over_heat.entry(this, context)
    -- 仅播放一次的过热时触发的动画,比如大量冒烟和警报声
    context:runAnimation("over_heat", context:getTrack(BLENDING_TRACK_LINE, OVER_HEAT_TRACK), true, PLAY_ONCE_STOP, 0.2)
    -- 一直循环执行的过热触发动画,比如缓慢的冒烟
    context:runAnimation("static_over_heating", context:getTrack(BLENDING_TRACK_LINE, OVER_HEATING_TRACK), true, LOOP, 0)
end

-- 更新"过热"状态
function over_heat_states.over_heat.update(this, context)
    if (not isOverHeat(context)) then
        context:trigger(this.INPUT_COOLING_HEAT)
    end
end

-- 转出"过热"状态
function over_heat_states.over_heat.transition(this, context, input)
    -- 如果收到了来自上文 update 方法的输入,则转到"不过热"状态
    if (input == this.INPUT_COOLING_HEAT) then
        -- 由于并没有一个"不过热"的动画,因此必须在这里把过热动画停止了才能转到"不过热"状态
        context:stopAnimation(context:getTrack(BLENDING_TRACK_LINE, OVER_HEATING_TRACK))
        return this.over_heat_states.normal
    end
end
-- 结束过热部分

local ADS_states = {
    aiming_progress = 0,-- 记录瞄准进度
    normal = {},-- 不瞄准状态
    aiming = {}-- 瞄准状态
}

-- 进入不瞄准状态
function ADS_states.normal.entry(this, context)
    this.ADS_states.normal.update(this, context)
end

-- 更新不瞄准状态
function ADS_states.normal.update(this, context)
    -- 当瞄准进度正在增加时转到瞄准状态
    if ((context:getAimingProgress() > this.ADS_states.aiming_progress or context:getAimingProgress() == 1) and context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:trigger(this.INPUT_AIM)
    else
        -- 如果没有增加，则记录当前的瞄准进度
        this.ADS_states.aiming_progress = context:getAimingProgress()
    end
end

-- 转出不瞄准状态
function ADS_states.normal.transition(this, context, input)
    -- 接收到上文 update 方法的输入，则转到瞄准状态
    if (input == this.INPUT_AIM) then
        return this.ADS_states.aiming
    end
end

-- 进入瞄准状态
function ADS_states.aiming.entry(this, context)
    -- 开始瞄准时播放瞄准动画，并且将其挂起
    local track = context:getTrack(STATIC_TRACK_LINE, ADS_TRACK)
    context:runAnimation("aim_start", track, false, PLAY_ONCE_HOLD, 0.2)
    -- 打断检视动画
    context:trigger(this.INPUT_INSPECT_RETREAT)
end

-- 更新瞄准状态
function ADS_states.aiming.update(this, context)
    local track = context:getTrack(STATIC_TRACK_LINE, ADS_TRACK)
    if (context:isHolding(track)) then
        -- 循环播放瞄准时的动画
        context:runAnimation("aim", track, false, PLAY_ONCE_HOLD, 0.2)
    end
    -- 当瞄准进度正在减小时转到不瞄准状态，也即取消瞄准
    if (context:getAimingProgress() < this.ADS_states.aiming_progress or not context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:trigger(this.INPUT_AIM_RETREAT)
    else
        -- 如果没有减小，则记录当前瞄准进度
        this.ADS_states.aiming_progress = context:getAimingProgress()
    end
end

-- 转出瞄准状态
function ADS_states.aiming.transition(this, context, input)
    local track = context:getTrack(STATIC_TRACK_LINE, ADS_TRACK)
    if (input == this.INPUT_AIM_RETREAT) then
        --播放瞄准结束动画，并调整动画进度使开镜动画与当前的开镜进度相对应
        context:runAnimation("aim_end", track, false, PLAY_ONCE_STOP, 0.2)
        context:setAnimationProgress(track, 1 - context:getAimingProgress(), true)
        return this.ADS_states.normal
    end
end

local M = {
    -- 轨道行
    track_line_top = track_line_top,
    STATIC_TRACK_LINE = STATIC_TRACK_LINE,
    GUN_KICK_TRACK_LINE = GUN_KICK_TRACK_LINE,
    BLENDING_TRACK_LINE = BLENDING_TRACK_LINE,
    -- 静态轨道
    static_track_top = static_track_top,
    BASE_TRACK = BASE_TRACK,
    BOLT_CAUGHT_TRACK = BOLT_CAUGHT_TRACK,
    SAFETY_TRACK = SAFETY_TRACK,
    ADS_TRACK = ADS_TRACK,
    MAIN_TRACK = MAIN_TRACK,
    SPRINT_TRACK = SPRINT_TRACK,
    -- 混合轨道
    blending_track_top = blending_track_top,
    MOVEMENT_TRACK = MOVEMENT_TRACK,
    SLIDE_TRACK = SLIDE_TRACK,
    OVER_HEAT_TRACK = OVER_HEAT_TRACK,
    OVER_HEATING_TRACK = OVER_HEATING_TRACK,
    LOOP_TRACK = LOOP_TRACK,
    -- 低级并行轨道
    PRE_PARALLEL_TRACK_1 = PRE_PARALLEL_TRACK_1,
    PRE_PARALLEL_TRACK_2 = PRE_PARALLEL_TRACK_2,
    PRE_PARALLEL_TRACK_3 = PRE_PARALLEL_TRACK_3,
    PRE_PARALLEL_TRACK_4 = PRE_PARALLEL_TRACK_4,
    PRE_PARALLEL_TRACK_5 = PRE_PARALLEL_TRACK_5,
    -- 顶级并行轨道
    PARALLEL_TRACK_1 = PARALLEL_TRACK_1,
    PARALLEL_TRACK_2 = PARALLEL_TRACK_2,
    PARALLEL_TRACK_3 = PARALLEL_TRACK_3,
    PARALLEL_TRACK_4 = PARALLEL_TRACK_4,
    PARALLEL_TRACK_5 = PARALLEL_TRACK_5,
    -- 混合轨道
    BLEND_TRACK_1 = BLEND_TRACK_1,
    BLEND_TRACK_2 = BLEND_TRACK_2,
    BLEND_TRACK_3 = BLEND_TRACK_3,
    BLEND_TRACK_4 = BLEND_TRACK_4,
    BLEND_TRACK_5 = BLEND_TRACK_5,
    -- 状态
    base_track_state = base_track_state,
    bolt_caught_states = bolt_caught_states,
    over_heat_states = over_heat_states,
    main_track_states = main_track_states,
    gun_kick_state = gun_kick_state,
    movement_track_states = movement_track_states,
    ADS_states = ADS_states,
    slide_states = slide_states,
    -- 输入
    INPUT_BOLT_CAUGHT = "bolt_caught",
    INPUT_BOLT_NORMAL = "bolt_normal",
    INPUT_OVER_HEAT = "over_heat",
    INPUT_COOLING_HEAT = "cooling_heat",
    INPUT_INSPECT_RETREAT = "inspect_retreat",
    INPUT_INSPECT_BREAK_OUT = "inspect_break_out",
    INPUT_CHARING = "input_charging",
    INPUT_CHARING_EXIT = "input_charging_exit",
    INPUT_AIM = "aim",
    INPUT_AIM_RETREAT = "aim_retreat"
}

-- 状态机初始化函数，在切枪的时候调用
function M:initialize(context)
    context:ensureTrackLineSize(track_line_top.value)
    context:ensureTracksAmount(STATIC_TRACK_LINE, static_track_top.value)
    context:ensureTracksAmount(BLENDING_TRACK_LINE, blending_track_top.value)
    self.main_track_states.charge.can_charge = true
    self.main_track_states.allow_run_overlay = false
    self.movement_track_states.run.mode = -1
    self.movement_track_states.walk.mode = -1
end

-- 状态机退出函数，在收枪的时候调用
function M:exit(context)
    -- do some cleaning up things
end

function M:states()
    return {
        self.base_track_state,
        self.bolt_caught_states.normal,
        self.over_heat_states.normal,
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle,
        self.ADS_states.normal,
        self.slide_states.normal
    }
end

return M