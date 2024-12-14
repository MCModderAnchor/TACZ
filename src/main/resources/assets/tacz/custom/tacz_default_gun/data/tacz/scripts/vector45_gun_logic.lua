-- 定义逻辑机，定式
local M = {}

-- 当开始换弹的时候会调用一次
function M.start_reload(api)
    -- cache 相当于一个缓存，这里面可以存一些数据
    -- 这里在开始换弹时定义缓存是在做初始化
    local cache = {
        -- 这是一个判断弹量的数值是否已经发生了变化的标志位， 0 是还没有装填， 1 是已经装填
        reloaded = 0,
        -- 这是判断当前这把枪需要装多少发
        needed_count = api:getNeededAmmoAmount(),
        -- 这是判断本次换弹是否为战术换弹
        is_tactical = api:getReloadStateType() == TACTICAL_RELOAD_FEEDING,
    }
    -- 把缓存里的东西写入玩家身上的数据，不写的话无法读取。这句可以当作定式
    api:cacheScriptData(cache)
    -- 这里必须返回 true 了之后才会开启之后的 tick_reload 方法，因此必须写
    return true
end

-- 这是个 lua 函数，用来从枪 data 文件里获取装弹相关的动画时间点，由于 lua 内的时间是毫秒，所以要和 1000 做乘算
local function getReloadTimingFromParam(param)
    local reload_feed = param.reload_feed * 1000
    local reload_cooldown = param.reload_cooldown * 1000
    local empty_feed = param.empty_feed * 1000
    local empty_cooldown = param.empty_cooldown * 1000
    local reload_xmag2_feed = param.reload_xmag2_feed * 1000
    local reload_xmag2_cooldown = param.reload_xmag2_cooldown * 1000
    local empty_xmag2_feed = param.empty_xmag2_feed * 1000
    local empty_xmag2_cooldown = param.empty_xmag2_cooldown * 1000
    local reload_xmag3_feed = param.reload_xmag3_feed * 1000
    local reload_xmag3_cooldown = param.reload_xmag3_cooldown * 1000
    local empty_xmag3_feed = param.empty_xmag3_feed * 1000
    local empty_xmag3_cooldown = param.empty_xmag3_cooldown * 1000
    -- 这两个判断是用来检查以上 12 个参数是否有缺失的，若有缺失则不获取任何参数。其实是可以写进一个判断语句的，但是这样的话整个句子会过长影响阅读所以我就拆成 3 个了
    if (reload_feed == nil or reload_cooldown == nil or empty_feed == nil or empty_cooldown == nil) then
        return nil
    end
    if (reload_xmag2_feed == nil or reload_xmag2_cooldown == nil or empty_xmag2_feed == nil or empty_xmag2_cooldown == nil) then
        return nil
    end
    if (reload_xmag3_feed == nil or reload_xmag3_cooldown == nil or empty_xmag3_feed == nil or empty_xmag3_cooldown == nil) then
        return nil
    end
    -- 顺序返回获取到的这 12 个参数
    return reload_feed, reload_cooldown, empty_feed, empty_cooldown, reload_xmag2_feed, reload_xmag2_cooldown, empty_xmag2_feed, empty_xmag2_cooldown, reload_xmag3_feed, reload_xmag3_cooldown, empty_xmag3_feed, empty_xmag3_cooldown
end

-- 在换弹过程中每一帧都会执行的事，注意这里的帧不是 mc 的 tick ，这个运行起来可比一秒跑 20 次的 tick 快太多了
function M.tick_reload(api)
    -- 从枪 data 文件中获取所有需要传入逻辑机的参数，注意此时的 param 是个列表，还不能直接拿来用
    local param = api:getScriptParams();
    -- 调用刚才的 lua 函数，把 param 里包含的八个参数依次赋值给我们新定义的变量
    local reload_feed, reload_cooldown, empty_feed, empty_cooldown, reload_xmag2_feed, reload_xmag2_cooldown, empty_xmag2_feed, empty_xmag2_cooldown, reload_xmag3_feed, reload_xmag3_cooldown, empty_xmag3_feed, empty_xmag3_cooldown = getReloadTimingFromParam(param)
    -- 获取换弹时间，在玩家按下 R 的一瞬间作为零点，单位是毫秒。假设玩家在一秒前按下了 R ，那么此时这个时间就是 1000
    local reload_time = api:getReloadTime()
    -- 从玩家身上获取脚本开头缓存的数据
    local cache = api:getCachedScriptData()
    -- 照例检查是否有参数缺失
    if (reload_feed == nil or reload_cooldown == nil or empty_feed == nil or empty_cooldown == nil) then
        return NOT_RELOADING, -1
    end
    if (reload_xmag2_feed == nil or reload_xmag2_cooldown == nil or empty_xmag2_feed == nil or empty_xmag2_cooldown == nil) then
        return NOT_RELOADING, -1
    end
    if (reload_xmag3_feed == nil or reload_xmag3_cooldown == nil or empty_xmag3_feed == nil or empty_xmag3_cooldown == nil) then
        return NOT_RELOADING, -1
    end

    -- 判断此时玩家手里拿的枪是不是 0 级扩容
    if (api:getMagExtentLevel() == 0) then
        -- 0 级的战术换弹
        if (cache.is_tactical) then
            -- 当换弹时间还不到战术装填时间时返回 FEEDING 和距离装填时间节点的剩余时间
            if (reload_time < reload_feed) then
                return TACTICAL_RELOAD_FEEDING, reload_feed - reload_time
            -- 当换弹时间达到了战术装填时间但是又没有完成整个流程时返回 FINISHING 和距离结束时间节点的剩余时间
            elseif (reload_time >= reload_feed and reload_time < reload_cooldown) then
                -- 因为装填动作只进行一次而脚本却每一帧都在跑，所以需要一个标志位告诉我“装填”这一动作是否已经执行过了
                if (cache.reloaded ~= 1) then
                    -- 判断玩家装弹是否需要消耗弹药，约等于检测玩家是不是创造模式
                    if (api:isReloadingNeedConsumeAmmo()) then
                        -- 需要消耗弹药（生存或冒险）的话就消耗换弹所需的弹药并将消耗的数量装填进弹匣
                        api:putAmmoInMagazine(api:consumeAmmoFromPlayer(cache.needed_count))
                    else
                        -- 不需要消耗弹药（创造）的话就直接把弹匣塞满
                        api:putAmmoInMagazine(cache.needed_count)
                    end
                    -- 更改装填标志位，这样脚本下一次运行的时候就知道已经装过了
                    cache.reloaded = 1
                end
                -- 在这个时间段要返回 FINISHING 和剩余时间
                return TACTICAL_RELOAD_FINISHING, reload_cooldown - reload_time
            else
                -- 在以上两种情况之外（已经完成换弹）返回没有在装填，剩余时间置于 -1
                return NOT_RELOADING, -1
            end

        -- 0 级的空仓换弹
        else
            -- 当换弹时间还不到战术装填时间时返回 FEEDING 和距离装填时间节点的剩余时间
            if (reload_time < empty_feed) then
                return EMPTY_RELOAD_FEEDING, empty_feed - reload_time
                -- 当换弹时间达到了空仓装填时间但是又没有完成整个流程时返回 FINISHING 和距离结束时间节点的剩余时间
            elseif (reload_time >= empty_feed and reload_time < empty_cooldown) then
                -- 检查装填标志位
                if (cache.reloaded ~= 1) then
                    -- 检查游戏模式
                    if (api:isReloadingNeedConsumeAmmo()) then
                        -- 注意空仓换弹的装填数量不能和消耗量划等号，必须 -1 ，原因下面会说（ 108 行）
                        api:putAmmoInMagazine(api:consumeAmmoFromPlayer(cache.needed_count) - 1)
                    else
                        -- 创造模式下往弹匣内塞子弹，同理 - 1
                        api:putAmmoInMagazine(cache.needed_count - 1)
                    end
                    -- 一把枪的子弹分为“枪管内”和“弹匣内”这两部分，上文所有操作都是针对弹匣的
                    -- 假设一把枪的弹容是 30 + 1 发 ，那么弹匣容量为 30 ，枪管容量为 1
                    -- 由于战术换弹时整枪一定为 x + 1 发子弹，也就是说枪管内一定是有子弹的，因此战术换弹时我们不需要对枪管操作
                    -- 但是空仓时整个枪是 0 + 0 ，此时换弹需要消耗 30 发子弹把枪变成 29 + 1
                    -- 因此这里的操作应该是“往弹匣内填充 29 发”，“往枪管内填充 1 发”，“消耗 30 发”
                    -- 由于往弹匣内填充的量比消耗的少 1 发，所以上文（ 103 行）需要 - 1
                    -- 同时下面这句的意思是往枪管内填充 1 发子弹
                    api:setAmmoInBarrel(true)
                    -- 更改装填标志位
                    cache.reloaded = 1
                end
                -- 在这个时间段要返回 FINISHING 和剩余时间
                return EMPTY_RELOAD_FINISHING, empty_cooldown - reload_time
            else
                -- 在以上两种情况之外（已经完成换弹）返回没有在装填，剩余时间置于 -1
                return NOT_RELOADING, -1
            end
        end

    elseif (api:getMagExtentLevel() == 1) then
        if (cache.is_tactical) then
            if (reload_time < reload_xmag2_feed) then
                return TACTICAL_RELOAD_FEEDING, reload_xmag2_feed - reload_time
            elseif (reload_time >= reload_xmag2_feed and reload_time < reload_xmag2_cooldown) then
                if (cache.reloaded ~= 1) then
                    if (api:isReloadingNeedConsumeAmmo()) then
                        api:putAmmoInMagazine(api:consumeAmmoFromPlayer(cache.needed_count))
                    else
                        api:putAmmoInMagazine(cache.needed_count)
                    end
                    cache.reloaded = 1
                end
                return TACTICAL_RELOAD_FINISHING, reload_xmag2_cooldown - reload_time
            else
                return NOT_RELOADING, -1
            end

        else
            if (reload_time < empty_xmag2_feed) then
                return EMPTY_RELOAD_FEEDING, empty_xmag2_feed - reload_time
            elseif (reload_time >= empty_xmag2_feed and reload_time < empty_xmag2_cooldown) then
                if (cache.reloaded ~= 1) then
                    if (api:isReloadingNeedConsumeAmmo()) then
                        api:putAmmoInMagazine(api:consumeAmmoFromPlayer(cache.needed_count) - 1)
                    else
                        api:putAmmoInMagazine(cache.needed_count - 1)
                    end
                    api:setAmmoInBarrel(true)
                    cache.reloaded = 1
                end
                return EMPTY_RELOAD_FINISHING, empty_xmag2_cooldown - reload_time
            else
                return NOT_RELOADING, -1
            end
        end

    elseif ((api:getMagExtentLevel() == 2 or api:getMagExtentLevel() == 3)) then
        if (cache.is_tactical) then
            if (reload_time < reload_xmag3_feed) then
                return TACTICAL_RELOAD_FEEDING, reload_xmag3_feed - reload_time
            elseif (reload_time >= reload_xmag3_feed and reload_time < reload_xmag3_cooldown) then
                if (cache.reloaded ~= 1) then
                    if (api:isReloadingNeedConsumeAmmo()) then
                        api:putAmmoInMagazine(api:consumeAmmoFromPlayer(cache.needed_count))
                    else
                        api:putAmmoInMagazine(cache.needed_count)
                    end
                    cache.reloaded = 1
                end
                return TACTICAL_RELOAD_FINISHING, reload_xmag3_cooldown - reload_time
            else
                return NOT_RELOADING, -1
            end

        else
            if (reload_time < empty_xmag3_feed) then
                return EMPTY_RELOAD_FEEDING, empty_xmag3_feed - reload_time
            elseif (reload_time >= empty_xmag3_feed and reload_time < empty_xmag3_cooldown) then
                if (cache.reloaded ~= 1) then
                    if (api:isReloadingNeedConsumeAmmo()) then
                        api:putAmmoInMagazine(api:consumeAmmoFromPlayer(cache.needed_count) - 1)
                    else
                        api:putAmmoInMagazine(cache.needed_count - 1)
                    end
                    api:setAmmoInBarrel(true)
                    cache.reloaded = 1
                end
                return EMPTY_RELOAD_FINISHING, empty_xmag3_cooldown - reload_time
            else
                return NOT_RELOADING, -1
            end
        end
    end
end

-- 向模组返回整个逻辑机，定式
return M