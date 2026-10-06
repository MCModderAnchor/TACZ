local M = {}

function M.start_reload(api)
    -- 初始化缓存里会用到的参数
    local cache = {
        reloaded_count = 0,
        needed_count = api:getNeededAmmoAmount(),
        is_tactical = api:getReloadStateType() == TACTICAL_RELOAD_FEEDING,
        interrupted_time = -1,
    }
    api:cacheScriptData(cache)
    -- 返回 true 开始 tick
    return true
end

local function getReloadTimingFromParam(param)
    -- 将时间转化为毫秒制
    local intro_empty = param.intro_empty * 1000
    local intro = param.intro * 1000
    local loop = param.loop * 1000
    local loop_2 = param.loop_2 * 1000
    local ending = param.ending * 1000
    local intro_empty_feed = param.intro_empty_feed * 1000
    local loop_feed = param.loop_feed * 1000
    local loop_feed_2 = param.loop_feed_2 * 1000
    -- 检查是否任意时间为空值
    if (intro_empty == nil or intro == nil or loop == nil or loop_2 == nil or ending == nil or intro_empty_feed == nil or loop_feed == nil or loop_feed_2 == nil) then
        return nil
    end
    -- 依次返回时间
    return intro_empty, intro, loop, loop_2, ending, intro_empty_feed, loop_feed, loop_feed_2
end

function M.tick_reload(api)
    -- 从枪的 data 文件里获取所有脚本需要的参数值
    local param = api:getScriptParams();
    local intro_empty, intro, loop, loop_2, ending, intro_empty_feed, loop_feed, loop_feed_2 = getReloadTimingFromParam(param)
    -- 象征性的检查一下时间有没有空值
    if (intro_empty == nil) then
        return NOT_RELOADING, -1
    end
    -- 获取换弹时间（从按下 R 到当前）
    local reload_time = api:getReloadTime()
    -- 获取预先缓存的参数
    local cache = api:getCachedScriptData()
    -- 缓存由 start_reload 建立。切枪时引擎可能拿着新枪来 tick 上一把枪遗留的换弹状态，
    -- 此时新枪的 start_reload 从未执行过，缓存为空——直接取字段会抛 LuaError 崩客户端。
    if (cache == nil) then
        return NOT_RELOADING, -1
    end
    local interrupted_time = cache.interrupted_time
    -- 打断换弹
    if (interrupted_time ~= -1) then
        local int_time = reload_time - interrupted_time
        if (int_time >= ending) then
            return NOT_RELOADING, -1
        else
            if (cache.is_tactical) then
                return TACTICAL_RELOAD_FINISHING, ending - int_time
            else
                return EMPTY_RELOAD_FINISHING, ending - int_time
            end
        end
    else
        -- 如果玩家背包里已经没有可以消耗的弹药也打断换弹
        if (not api:hasAmmoToConsume()) then
            interrupted_time = api:getReloadTime()
        end
    end
    -- 空仓换弹往枪管里塞 1 颗
    local reloaded_count = cache.reloaded_count;
    if (reloaded_count == 0) then
        if (not cache.is_tactical) then
            if (reload_time > intro_empty_feed) then
                api:consumeAmmoFromPlayer(1)
                api:setAmmoInBarrel(true)
                reloaded_count = reloaded_count + 1
            end
        else
            reloaded_count = reloaded_count + 1
        end
    end
    -- 循环换弹
    if (reloaded_count > 0) then
        local base_time = 0
        -- 如果需要的弹药量为 1 则只唤起一次 loop
        if (cache.needed_count == 1) then
            base_time = 0 + loop_feed
        -- 如果需要的弹药量大于 1 则需要唤起 x 次 loop_2 和 0/1 次 loop
        elseif (cache.needed_count > 1) then
            -- 根据装弹数量分别计算两种情况下的下一次 feed 距离换弹起始点的时长（偶数为 x-1 次 loop_2 循环，奇数为 y 次 loop_2 和 1 次 loop 的时长）
            if (reloaded_count % 2 == 0) then
                base_time = ((reloaded_count - 2) / 2) * loop_2 + loop_feed_2
            else
                base_time = ((reloaded_count - 1) / 2) * loop_2 + loop_feed
            end
        end
        -- 在距离下一次 feed 的时长基础上添加起始时间
        if (not cache.is_tactical) then
            base_time = base_time + intro_empty
        else
            base_time = base_time + intro
        end
        -- 换弹时间达到了下一次 feed 时
        while (base_time < reload_time) do
            -- 如果换弹需求已满足则退出循环
            if (reloaded_count > cache.needed_count) then
                break
            end
            -- 如果需求量大于等于 2 ，则双发装填
            if (cache.needed_count - reloaded_count >= 1) then
                reloaded_count = reloaded_count + 2
                base_time = base_time + loop_2
                -- 判断玩家所处的游戏模式
                if (api:isReloadingNeedConsumeAmmo()) then
                    api:putAmmoInMagazine(api:consumeAmmoFromPlayer(2))
                else
                    api:putAmmoInMagazine(2)
                end
            -- 如果需求量等于 1 ，则单发装填
            elseif (cache.needed_count - reloaded_count < 1) then
                reloaded_count = reloaded_count + 1
                base_time = base_time + loop
                -- 判断玩家所处的游戏模式
                if (api:isReloadingNeedConsumeAmmo()) then
                    api:putAmmoInMagazine(api:consumeAmmoFromPlayer(1))
                else
                    api:putAmmoInMagazine(1)
                end
            end
        end
    end

    -- 将数据写回缓存
    if (reloaded_count > cache.needed_count) then
        interrupted_time = api:getReloadTime() - loop_feed + loop
    end
    cache.interrupted_time = interrupted_time
    cache.reloaded_count = reloaded_count
    api:cacheScriptData(cache)
    -- 返回换弹状态，这里的 total_time 是任意状态下换弹的总时长（不包含 ending 的时间）
    local total_time = ((cache.needed_count - (cache.needed_count % 2)) / 2) * loop_2 + (cache.needed_count % 2) * loop
    if (not cache.is_tactical) then
        total_time = total_time + intro_empty
        return EMPTY_RELOAD_FEEDING, total_time - reload_time
    else
        total_time = total_time + intro
        return TACTICAL_RELOAD_FEEDING, total_time - reload_time
    end
end

function M.interrupt_reload(api)
    local cache = api:getCachedScriptData()
    if (cache ~= nil and cache.interrupted_time == -1) then
        cache.interrupted_time = api:getReloadTime()
    end
end

return M