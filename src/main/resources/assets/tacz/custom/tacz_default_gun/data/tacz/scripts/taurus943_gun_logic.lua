local M = {}

function M.shoot(api)
    api:shootOnce(api:isShootingNeedConsumeAmmo())
end

-- 左轮为开膛待击（open_bolt），没有独立枪膛，弹巢就是弹匣（最多 8 发）。
-- 引擎只在 manual_action 时才调用 start_bolt/tick_bolt，所以这里不需要拉栓逻辑。

local function isValidTime(value)
    return type(value) == "number" and value >= 0 and value * 1000 < math.huge
end

-- unload_magazine == false 表示不退弹；未配置 unload_amount 时退光弹巢。
local function getUnloadAmount(param)
    if (param.unload_magazine == false) then
        return 0
    end
    if (param.unload_amount == nil) then
        return -1
    end
    return param.unload_amount
end

function M.start_reload(api)
    local param = api:getScriptParams()
    local intro = param.intro
    local loop = param.loop
    local loop_feed = param.loop_feed
    local ending = param.ending
    local unload_time = param.unload_time
    local unload_amount = getUnloadAmount(param)
    -- 左轮只有一套换弹流程：intro（期间退弹）→ 逐发装填 → ending。
    if (not isValidTime(intro) or not isValidTime(loop) or loop <= 0
            or not isValidTime(loop_feed) or not isValidTime(ending)
            or not isValidTime(unload_time)) then
        return false
    end
    if (type(unload_amount) ~= "number" or unload_amount < -1 or unload_amount > 2147483647
            or unload_amount % 1 ~= 0) then
        return false
    end
    -- 退弹必须发生在 intro 内、第一发装填结算之前，否则弹药结算会与动画错位。
    if (unload_time > intro) then
        return false
    end
    local cache = {
        reloaded_count = 0,
        -- 退弹前先用弹巢容量估算剩余时间，退弹后再按实际空位修正。
        needed_count = api:getMaxAmmoCount(),
        interrupted_time = -1,
        unloaded = false,
        unload_amount = unload_amount,
        unload_time = unload_time * 1000,
    }
    api:cacheScriptData(cache)
    return true
end

-- 读取换弹动画时间（秒转换为毫秒），配置不完整时返回 nil。
local function getReloadTiming(param)
    local intro = param.intro
    local loop = param.loop
    local loop_feed = param.loop_feed
    local ending = param.ending
    if (not isValidTime(intro) or not isValidTime(loop) or loop <= 0
            or not isValidTime(loop_feed) or not isValidTime(ending)) then
        return nil
    end
    return intro * 1000, loop * 1000, loop_feed * 1000, ending * 1000
end

function M.tick_reload(api)
    -- 切枪时引擎可能拿着新枪来 tick 上一把枪遗留的换弹状态，此时缓存为空。
    local cache = api:getCachedScriptData()
    if (cache == nil) then
        return NOT_RELOADING, -1
    end
    local param = api:getScriptParams()
    local intro, loop, loop_feed, ending = getReloadTiming(param)
    if (intro == nil) then
        return NOT_RELOADING, -1
    end
    local reload_time = api:getReloadTime()

    -- 收尾阶段：主动取消、备弹耗尽、弹巢装满都会预约一个收尾时刻。
    local interrupted_time = cache.interrupted_time
    if (interrupted_time ~= -1) then
        if (reload_time < interrupted_time) then
            -- 收尾时刻未到，继续停留在装填阶段，等当前 loop 动画播完。
            return EMPTY_RELOAD_FEEDING, interrupted_time - reload_time
        end
        local int_time = reload_time - interrupted_time
        if (int_time >= ending) then
            return NOT_RELOADING, -1
        end
        return EMPTY_RELOAD_FINISHING, ending - int_time
    end

    -- 退弹：intro 期间到达 unload_time 时，一次性退出弹巢里的子弹。
    if (not cache.unloaded) then
        if (reload_time < cache.unload_time) then
            return EMPTY_RELOAD_FEEDING, intro + cache.needed_count * loop - reload_time
        end
        local unload_amount = cache.unload_amount
        if (unload_amount ~= 0) then
            -- 左轮没有独立枪膛，barrel 固定传 0；弹巢就是弹匣。
            local magazine_count = math.max(0, api:getAmmoCountInMagazine())
            local expected = unload_amount == -1 and magazine_count
                    or math.min(magazine_count, unload_amount)
            local unloaded = api:unloadAmmo(0, unload_amount)
            if (unloaded ~= expected) then
                -- 退弹失败立即收尾，避免把残留的子弹当成空弹巢继续装填。
                cache.interrupted_time = reload_time
                api:cacheScriptData(cache)
                return EMPTY_RELOAD_FINISHING, ending
            end
        end
        cache.unloaded = true
        -- 退弹完成后按实际空位计算需要装填的数量。
        cache.needed_count = api:getNeededAmmoAmount()
        if (cache.needed_count <= 0) then
            cache.interrupted_time = reload_time
            api:cacheScriptData(cache)
            return EMPTY_RELOAD_FINISHING, ending
        end
    end

    -- 逐发装填：第 k 发在 intro 结束后的第 (k-1) 个 loop 内、loop_feed 时刻结算。
    local reloaded_count = cache.reloaded_count
    local no_ammo = not api:hasAmmoToConsume()
    while (not no_ammo and reloaded_count < cache.needed_count
            and reload_time >= intro + loop_feed + reloaded_count * loop) do
        local consumed = api:isReloadingNeedConsumeAmmo() and api:consumeAmmoFromPlayer(1) or 1
        if (consumed <= 0) then
            no_ammo = true
            break
        end
        reloaded_count = reloaded_count + 1
        api:putAmmoInMagazine(consumed)
        no_ammo = not api:hasAmmoToConsume()
    end
    cache.reloaded_count = reloaded_count

    -- 装填结束时刻：装满时等最后一圈转完；备弹耗尽时等当前这一圈转完。
    local finish_start
    if (reloaded_count >= cache.needed_count) then
        finish_start = intro + cache.needed_count * loop
    elseif (no_ammo) then
        finish_start = intro + reloaded_count * loop
    else
        -- 还没到下一发的装填时刻，保持装填状态等待。
        api:cacheScriptData(cache)
        return EMPTY_RELOAD_FEEDING, intro + cache.needed_count * loop - reload_time
    end
    cache.interrupted_time = math.max(reload_time, finish_start)
    api:cacheScriptData(cache)
    if (finish_start > reload_time) then
        return EMPTY_RELOAD_FEEDING, finish_start - reload_time
    end
    return EMPTY_RELOAD_FINISHING, ending
end

function M.interrupt_reload(api)
    local cache = api:getCachedScriptData()
    if (cache == nil) then
        return
    end
    local reload_time = api:getReloadTime()
    -- 已在收尾则不再改动；若还在等待预约的收尾时刻，则立即开始收尾。
    if (cache.interrupted_time == -1 or cache.interrupted_time > reload_time) then
        cache.interrupted_time = reload_time
        api:cacheScriptData(cache)
    end
end

return M
