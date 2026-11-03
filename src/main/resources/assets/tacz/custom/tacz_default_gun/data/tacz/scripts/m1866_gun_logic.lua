local M = {}

function M.shoot(api)
    api:shootOnce(api:isShootingNeedConsumeAmmo())
end

function M.start_bolt(api)
    return true
end

function M.tick_bolt(api)
    local params = api:getScriptParams()
    local total_bolt_time = 0
    local bolt_feed_time = 0
    if (api:getAimingProgress() < 0.5) then
        total_bolt_time = params.bolt_fast_time * 1000
        bolt_feed_time = params.bolt_feed_fast_time * 1000
    else
        total_bolt_time = params.bolt_time * 1000
        bolt_feed_time = params.bolt_feed_time * 1000
    end
    if (total_bolt_time == nil or bolt_feed_time == nil) then
        return false
    end
    local bolt_time = api:getBoltTime()
    if (bolt_time < bolt_feed_time) then
        return true
    else
        if (not api:hasAmmoInBarrel()) then
            if (api:removeAmmoFromMagazine(1) ~= 0) then
                api:setAmmoInBarrel(true)
            end
        end
        return bolt_time < total_bolt_time
    end
end

function M.start_reload(api)
    local cache = {
        reloaded_count = 0,
        needed_count = api:getNeededAmmoAmount(),
        is_tactical = api:getReloadStateType() == TACTICAL_RELOAD_FEEDING,
        interrupted_time = -1,
        did_chamber = false,
    }
    api:cacheScriptData(cache)
    return true
end

local function getReloadTimingFromParam(param)
    local intro_empty = param.intro_empty * 1000
    local intro = param.intro * 1000
    local loop = param.loop * 1000
    local ending = param.ending * 1000
    local ending_empty = param.ending_empty * 1000
    local ending_empty_feed = param.ending_empty_feed * 1000
    local loop_feed = param.loop_feed * 1000
    if (intro_empty == nil or intro == nil or loop == nil or ending == nil or ending_empty == nil or ending_empty_feed == nil or loop_feed == nil) then
        return nil
    end
    return intro_empty, intro, loop, ending, ending_empty, ending_empty_feed, loop_feed
end

function M.tick_reload(api)
    local param = api:getScriptParams()
    local intro_empty, intro, loop, ending, ending_empty, ending_empty_feed, loop_feed = getReloadTimingFromParam(param)
    if (intro_empty == nil) then
        return NOT_RELOADING, -1
    end
    local reload_time = api:getReloadTime()
    local cache = api:getCachedScriptData()
    -- 缓存由 start_reload 建立。切枪时引擎可能拿着新枪来 tick 上一把枪遗留的换弹状态，
    -- 此时新枪的 start_reload 从未执行过，缓存为空——直接取字段会抛 LuaError 崩客户端。
    if (cache == nil) then
        return NOT_RELOADING, -1
    end
    local interrupted_time = cache.interrupted_time
    local ending_duration = cache.is_tactical and ending or ending_empty

    -- Handle finishing phase (reload complete or out of ammo)
    if (interrupted_time ~= -1) then
        local int_time = reload_time - interrupted_time
        -- During reload_end_empty, cycle lever to chamber a round from magazine
        if (not cache.is_tactical and not cache.did_chamber and int_time >= ending_empty_feed) then
            if (not api:hasAmmoInBarrel()) then
                if (api:removeAmmoFromMagazine(1) ~= 0) then
                    api:setAmmoInBarrel(true)
                end
            end
            cache.did_chamber = true
            api:cacheScriptData(cache)
        end
        if (int_time >= ending_duration) then
            return NOT_RELOADING, -1
        else
            if (cache.is_tactical) then
                return TACTICAL_RELOAD_FINISHING, ending_duration - int_time
            else
                return EMPTY_RELOAD_FINISHING, ending_duration - int_time
            end
        end
    else
        if (not api:hasAmmoToConsume()) then
            interrupted_time = api:getReloadTime()
        end
    end

    local reloaded_count = cache.reloaded_count
    local intro_duration = cache.is_tactical and intro or intro_empty

    -- During intro, no ammo is consumed — just opening the loading gate
    if (reloaded_count == 0 and reload_time < intro_duration) then
        cache.interrupted_time = interrupted_time
        cache.reloaded_count = reloaded_count
        api:cacheScriptData(cache)
        if (cache.is_tactical) then
            return TACTICAL_RELOAD_FEEDING, intro_duration - reload_time
        else
            return EMPTY_RELOAD_FEEDING, intro_duration - reload_time
        end
    end

    -- Loop phase: all rounds go into the magazine
    -- The first round is NOT chambered here — that happens during reload_end_empty
    local base_time = reloaded_count * loop + loop_feed + intro_duration
    while (base_time < reload_time) do
        if (reloaded_count > cache.needed_count) then
            break
        end
        reloaded_count = reloaded_count + 1
        base_time = base_time + loop
        api:consumeAmmoFromPlayer(1)
        api:putAmmoInMagazine(1)
    end

    if (reloaded_count >= cache.needed_count) then
        interrupted_time = api:getReloadTime() - loop_feed + loop
    end
    cache.interrupted_time = interrupted_time
    cache.reloaded_count = reloaded_count
    api:cacheScriptData(cache)

    local total_time = cache.needed_count * loop + intro_duration
    if (cache.is_tactical) then
        return TACTICAL_RELOAD_FEEDING, total_time - reload_time
    else
        return EMPTY_RELOAD_FEEDING, total_time - reload_time
    end
end

function M.interrupt_reload(api)
    local cache = api:getCachedScriptData()
    if (cache ~= nil and cache.interrupted_time == -1) then
        cache.interrupted_time = api:getReloadTime()
    end
end

return M
