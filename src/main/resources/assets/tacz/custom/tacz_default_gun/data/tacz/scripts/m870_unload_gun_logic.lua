local M = {}

function M.shoot(api)
    api:shootOnce(api:isShootingNeedConsumeAmmo())
end

function M.start_bolt(api)
    -- Return true to start ticking, since there are nothing needed to be check
    return true
end

function M.tick_bolt(api)
    -- Get total bolt time from script parameter in gun data
    local params = api:getScriptParams()
    local total_bolt_time = params.bolt_time * 1000
    local bolt_feed_time = params.bolt_feed_time * 1000
    if (total_bolt_time == nil or bolt_feed_time == nil) then
        return false
    end
    local bolt_time = api:getBoltTime()
    if (bolt_time < bolt_feed_time) then
        -- Bolt time less than total means we need to keep ticking, return true
        return true
    else
        -- Bolt time greater than total means that the bullet
        -- needs to be put from the magazine into the barrel, and then return false to end ticking.
        if (not api:hasAmmoInBarrel()) then
            if (api:removeAmmoFromMagazine(1) ~= 0) then
                api:setAmmoInBarrel(true);
            end
        end
        return bolt_time < total_bolt_time
    end
end

local function getUnloadAmount(param)
    if (param.unload_magazine == false) then
        return 0
    end
    if (param.unload_amount == nil) then
        return -1
    end
    return param.unload_amount
end

local function isValidUnloadTime(value)
    return type(value) == "number" and value >= 0 and value * 1000 < math.huge
end

function M.start_reload(api)
    local param = api:getScriptParams()
    if (param.unload_time ~= nil and not isValidUnloadTime(param.unload_time)) then
        return false
    end
    local unload_amount = getUnloadAmount(param)
    -- 可选卸弹示例：未配置 unload_time 时保持原有换弹流程。
    local unload_enabled = param.unload_time ~= nil and type(unload_amount) == "number"
            and unload_amount >= -1 and unload_amount <= 2147483647 and unload_amount % 1 == 0
            and (unload_amount ~= 0 or param.unload_barrel ~= false)
    local is_tactical = api:getReloadStateType() == TACTICAL_RELOAD_FEEDING
    if (unload_enabled) then
        is_tactical = api:hasAmmoInBarrel() and param.unload_barrel == false
        -- 卸弹必须在首次装填结算前完成，避免前置动作与弹药结算错位。
        local first_feed = param.intro_empty_feed
        if (is_tactical) then
            if (type(param.intro) ~= "number" or type(param.loop_feed) ~= "number") then
                return false
            end
            first_feed = param.intro + param.loop_feed
        end
        if (not isValidUnloadTime(first_feed) or param.unload_time >= first_feed) then
            return false
        end
    end
    local needed_count = api:getNeededAmmoAmount()
    if (unload_enabled) then
        needed_count = unload_amount == -1 and api:getMaxAmmoCount()
                or math.min(api:getMaxAmmoCount(), needed_count + unload_amount)
    end
    -- Initialize cache that will be used in reload ticking
    local cache = {
        reloaded_count = 0,
        needed_count = needed_count,
        is_tactical = is_tactical,
        interrupted_time = -1,
        unloaded = not unload_enabled,
        unload_enabled = unload_enabled,
        unload_time = unload_enabled and param.unload_time * 1000 or 0,
        unload_amount = unload_amount,
        unload_barrel = param.unload_barrel ~= false,
    }
    api:cacheScriptData(cache)
    -- Return true to start ticking
    return true
end

local function getReloadTimingFromParam(param)
    -- Need to convert time from seconds to milliseconds
    local intro_empty = param.intro_empty * 1000
    local intro = param.intro * 1000
    local loop = param.loop * 1000
    local ending = param.ending * 1000
    local intro_empty_feed = param.intro_empty_feed * 1000
    local loop_feed = param.loop_feed * 1000
    -- Check if any timing is nil
    if (intro_empty == nil or intro == nil or loop == nil or ending == nil or intro_empty_feed == nil or loop_feed == nil) then
        return nil
    end
    return intro_empty, intro, loop, ending, intro_empty_feed, loop_feed
end

function M.tick_reload(api)
    -- Get all timings from script parameter in gun data
    local param = api:getScriptParams();
    local intro_empty, intro, loop, ending, intro_empty_feed, loop_feed = getReloadTimingFromParam(param)
    if (intro_empty == nil) then
        return NOT_RELOADING, -1
    end
    -- Get reload time (The time from the start of reloading to the current time) from api
    local reload_time = api:getReloadTime()
    -- Get cache from api, it will be used to count loaded ammo, mark reload interruptions, etc.
    local cache = api:getCachedScriptData()
    -- 缓存由 start_reload 建立。切枪时引擎可能拿着新枪来 tick 上一把枪遗留的换弹状态，
    -- 此时新枪的 start_reload 从未执行过，缓存为空——直接取字段会抛 LuaError 崩客户端。
    if (cache == nil) then
        return NOT_RELOADING, -1
    end
    local interrupted_time = cache.interrupted_time
    -- Handle interrupting reload
    if (interrupted_time ~= -1) then
        -- 自动装满/备弹耗尽可预约到本轮动画结束才收尾；主动取消仍立即执行。
        if (cache.unload_enabled and reload_time < interrupted_time) then
            local state_type = cache.is_tactical and TACTICAL_RELOAD_FEEDING or EMPTY_RELOAD_FEEDING
            return state_type, interrupted_time - reload_time
        end
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
    end
    -- 先到达脚本指定的卸弹时刻，再检查备弹；允许把枪内余弹退回后重新装入。
    if (not cache.unloaded) then
        if (reload_time < cache.unload_time) then
            local intro_time = cache.is_tactical and intro or intro_empty
            local state_type = cache.is_tactical and TACTICAL_RELOAD_FEEDING or EMPTY_RELOAD_FEEDING
            return state_type, intro_time + cache.needed_count * loop - reload_time
        end
        local unload_amount = cache.unload_amount
        local magazine_count = math.max(0, api:getAmmoCountInMagazine())
        local expected = unload_amount == -1 and magazine_count or math.min(magazine_count, unload_amount)
        if (cache.unload_barrel and api:hasAmmoInBarrel()) then
            expected = expected + 1
        end
        local unloaded = api:unloadAmmo(cache.unload_barrel and 1 or 0, unload_amount)
        if (unloaded ~= expected) then
            -- 卸弹失败立即收尾，不再消费备弹，也不覆盖尚未卸出的膛内一发。
            cache.interrupted_time = reload_time
            api:cacheScriptData(cache)
            local state_type = cache.is_tactical and TACTICAL_RELOAD_FINISHING or EMPTY_RELOAD_FINISHING
            return state_type, ending
        end
        cache.unloaded = true
        cache.needed_count = api:getNeededAmmoAmount()
    end
    if (not api:hasAmmoToConsume()) then
        cache.interrupted_time = reload_time
        if (cache.unload_enabled) then
            local intro_time = cache.is_tactical and intro or intro_empty
            cache.interrupted_time = math.max(reload_time, intro_time + math.max(0, cache.reloaded_count - 1) * loop)
            if (cache.interrupted_time > reload_time) then
                local state_type = cache.is_tactical and TACTICAL_RELOAD_FEEDING or EMPTY_RELOAD_FEEDING
                return state_type, cache.interrupted_time - reload_time
            end
        end
        local state_type = cache.is_tactical and TACTICAL_RELOAD_FINISHING or EMPTY_RELOAD_FINISHING
        return state_type, ending
    end
    -- Put an ammo into the barrel first
    local reloaded_count = cache.reloaded_count;
    if (reloaded_count == 0) then
        if (api:hasAmmoInBarrel()) then
            reloaded_count = reloaded_count + 1
        elseif (not cache.is_tactical) then
            if (reload_time > intro_empty_feed) then
                local consumed = api:isReloadingNeedConsumeAmmo() and api:consumeAmmoFromPlayer(1) or 1
                if (consumed > 0) then
                    api:setAmmoInBarrel(true)
                    reloaded_count = reloaded_count + 1
                else
                    interrupted_time = reload_time
                end
            end
        else
            reloaded_count = reloaded_count + 1
        end
    end
    -- Load the ammo into the magazine one by one
    if (reloaded_count > 0) then
        local base_time = (reloaded_count -1) * loop + loop_feed
        if (not cache.is_tactical) then
            base_time = base_time + intro_empty
        else
            base_time = base_time + intro
        end
        while (base_time < reload_time) do
            if (reloaded_count > cache.needed_count) then
                break
            end
            local consumed = api:isReloadingNeedConsumeAmmo() and api:consumeAmmoFromPlayer(1) or 1
            if (consumed <= 0) then
                interrupted_time = reload_time
                break
            end
            reloaded_count = reloaded_count + 1
            base_time = base_time + loop
            api:putAmmoInMagazine(consumed)
        end
    end
    -- Write back cache
    if (reloaded_count > cache.needed_count) then
        interrupted_time = api:getReloadTime() - loop_feed + loop
        if (cache.unload_enabled) then
            local intro_time = cache.is_tactical and intro or intro_empty
            interrupted_time = math.max(reload_time, intro_time + (reloaded_count - 1) * loop)
        end
    end
    cache.interrupted_time = interrupted_time
    cache.reloaded_count = reloaded_count
    api:cacheScriptData(cache)
    -- return reloadstate
    local total_time = cache.needed_count * loop
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
    if (cache ~= nil and (cache.interrupted_time == -1
            or (cache.unload_enabled and cache.interrupted_time > api:getReloadTime()))) then
        cache.interrupted_time = api:getReloadTime()
    end
end

return M
