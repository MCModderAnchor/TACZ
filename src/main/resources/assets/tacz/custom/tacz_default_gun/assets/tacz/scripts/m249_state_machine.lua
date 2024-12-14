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

-- 主轨道行 和 其中的五条轨道
local STATIC_TRACK_LINE = increment(track_line_top)
local BASE_TRACK = increment(static_track_top)
local BOLT_CAUGHT_TRACK = increment(static_track_top)
local SAFETY_TRACK = increment(static_track_top) -- 待实现
local ADS_TRACK = increment(static_track_top) -- 待实现
local MAIN_TRACK = increment(static_track_top)

-- 开火的轨道行
local GUN_KICK_TRACK_LINE = increment(track_line_top)

-- 混合轨道行 和 其中的两条轨道,用于叠加动画,如跑步走路跳跃, LOOP_TRACK 只有定义却尚未启用,因此作用尚不得知
local BLENDING_TRACK_LINE = increment(track_line_top)
local MOVEMENT_TRACK = increment(blending_track_top)
local LOOP_TRACK = increment(blending_track_top)

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

-- 基础轨道上的状态,这个状态用于循环播放 static_idle 动画。
local base_track_state = {}

-- 进入基础状态,直接播放 static_idle
function base_track_state.entry(this, context)
    -- 在 主轨道行 的 基础轨道 上循环播放 static_idle
    context:runAnimation("static_idle", context:getTrack(STATIC_TRACK_LINE, BASE_TRACK), false, LOOP, 0)
end

-- 空挂部分,该部分到 147 行结束
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

-- 进入"不空挂"状态
function bolt_caught_states.normal.entry(this, context)
    context:runAnimation("static_ammo_display", context:getTrack(STATIC_TRACK_LINE, BOLT_CAUGHT_TRACK), false, PLAY_ONCE_STOP, 0)
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

-- 主轨道状态,该部分到 256 行结束
-- 主轨道控制的是武器的基本动作,包括换弹,检视,刺刀攻击,掏枪,丢枪
-- 除了检视，其他动作不需要单独的状态控制。
-- 检视状态需要被射击输入打断，此外，进入检视时，需要隐藏准心，退出检视时恢复准心。
-- 除了上文的那三个方法，检视还需要 exit 方法，用于恢复准心渲染
local main_track_states = {
    -- 起始
    start = {},
    -- 闲置,当玩家把枪拿在手里站定并什么也不做的时候就是这种情况
    idle = {},
    -- 检视
    inspect = {},
    -- 结束
    final = {},
    -- 刺刀攻击的计数器
    bayonet_counter = 0
}

-- 转出 start (其实就是掏枪)
function main_track_states.start.transition(this, context, input)
    -- 玩家手里拿到枪的那一瞬间会自动输入一个 draw 的信号,不用手动触发
    if (input == INPUT_DRAW) then
        -- 收到 draw 信号后在主轨道行的主轨道上播放掏枪动画,然后转到闲置态
        context:runAnimation("draw", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, 0)
        return this.main_track_states.idle
    end
end

-- 转出闲置态
function main_track_states.idle.transition(this, context, input)
    -- 玩家从枪切到其他物品的时候会自动输入丢枪的信号,不用手动触发,只要检测就好了
    if (input == INPUT_PUT_AWAY) then
        runPutAwayAnimation(context)
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
    if (input == INPUT_INSPECT) then
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
    -- 退出后恢复屏幕中央准星
    context:setShouldHideCrossHair(false)
end

-- 更新检视态
function main_track_states.inspect.update(this, context)
    -- 当检测到动画停止了(播完了)时手动触发一次退出信号
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:trigger(this.INPUT_INSPECT_RETREAT)
    end
end

-- 转出检视态
function main_track_states.inspect.transition(this, context, input)
    -- 当收到来自 update 的退出信号时返回到闲置态,此时不需要停止动画是因为在 update 里是动画已经停止了才发出的退出信号
    if (input == this.INPUT_INSPECT_RETREAT) then
        return this.main_track_states.idle
    end
    -- 特殊地,射击应当打断检视,当检测到射击输入时应该直接停止动画并返回闲置态
    if (input == INPUT_SHOOT) then
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

-- 移动轨道的状态,这部分到 435 行结束
local movement_track_states = {
    -- 静止不动(或者在天上)
    idle = {},
    -- 奔跑, -1 是没有奔跑, 0 是在奔跑中
    run = {
        mode = -1
    },
    -- 行走, -1 是没有行走, 0 是在空中, 1 是正在瞄准, 2 是在向前走, 3 是向后退, 4 是向侧面走
    walk = {
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
    -- 如果玩家在奔跑则转去奔跑态
    if (input == INPUT_RUN) then
        return this.movement_track_states.run
    -- 如果玩家在行走则转去行走态
    elseif (input == INPUT_WALK) then
        return this.movement_track_states.walk
    end
end

-- 进入奔跑态
function movement_track_states.run.entry(this, context)
    this.movement_track_states.run.mode = -1
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
    -- 收到闲置输入则转去闲置态
    if (input == INPUT_IDLE) then
        return this.movement_track_states.idle
    -- 收到行走输入则转去行走态
    elseif (input == INPUT_WALK) then
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
    -- 收到闲置信号则转到闲置态
    if (input == INPUT_IDLE) then
        return this.movement_track_states.idle
    -- 收到奔跑信号则转到奔跑态
    elseif (input == INPUT_RUN) then
        return this.movement_track_states.run
    end
end
-- 结束移动轨道的状态

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
    -- 混合轨道
    blending_track_top = blending_track_top,
    MOVEMENT_TRACK = MOVEMENT_TRACK,
    LOOP_TRACK = LOOP_TRACK,
    -- 状态
    base_track_state = base_track_state,
    bolt_caught_states = bolt_caught_states,
    main_track_states = main_track_states,
    gun_kick_state = gun_kick_state,
    movement_track_states = movement_track_states,
    -- 输入
    INPUT_BOLT_CAUGHT = "bolt_caught",
    INPUT_BOLT_NORMAL = "bolt_normal",
    INPUT_INSPECT_RETREAT = "inspect_retreat"
}

-- 状态机初始化函数，在切枪的时候调用
function M:initialize(context)
    context:ensureTrackLineSize(track_line_top.value)
    context:ensureTracksAmount(STATIC_TRACK_LINE, static_track_top.value)
    context:ensureTracksAmount(BLENDING_TRACK_LINE, blending_track_top.value)
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
        self.main_track_states.start,
        self.gun_kick_state,
        self.movement_track_states.idle
    }
end

return M