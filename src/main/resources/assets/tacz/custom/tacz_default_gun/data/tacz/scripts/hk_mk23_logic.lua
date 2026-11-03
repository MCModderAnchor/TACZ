local M = {}
-- =========================================================== ↓ 换弹 ↓ =============
function M.start_reload(api)
    local cache = {
        cooldown = 0,
        feed = 0,
        loaded = false,
        load_flag = "NULL"
    }
    cache.loaded = false
    local param = api:getScriptParams();
    -- 空枪
    if (api:getAmmoAmount() == 0 and not api:hasAmmoInBarrel()) then
        cache.load_flag = "EMPTY"
        -- pump
        if (api:getFireMode() == SEMI) then
            -- pump lv 0
            if (api:getMagExtentLevel() == 0) then
                cache.cooldown = param.empty_pump_cooldown * 1000
                cache.feed = param.empty_pump_feed * 1000
            -- pump lv 1
            elseif (api:getMagExtentLevel() == 1) then
                cache.cooldown = param.empty_pump_xmag_1_cooldown * 1000
                cache.feed = param.empty_pump_xmag_1_feed * 1000
            -- pump lv 2 3
            elseif (api:getMagExtentLevel() == 2 or api:getMagExtentLevel() == 3) then
                cache.cooldown = param.empty_pump_xmag_2_cooldown * 1000
                cache.feed = param.empty_pump_xmag_2_feed * 1000
            end
        -- semi
        elseif (api:getFireMode() == BURST) then
            -- semi lv 0
            if (api:getMagExtentLevel() == 0) then
                cache.cooldown = param.empty_cooldown * 1000
                cache.feed = param.empty_feed * 1000
            -- semi lv 1
            elseif (api:getMagExtentLevel() == 1) then
                cache.cooldown = param.empty_xmag_1_cooldown * 1000
                cache.feed = param.empty_xmag_1_feed * 1000
            -- semi lv 2 3
            elseif (api:getMagExtentLevel() == 2 or api:getMagExtentLevel() == 3) then
                cache.cooldown = param.empty_xmag_2_cooldown * 1000
                cache.feed = param.empty_xmag_2_feed * 1000
            end
        end
    -- 战术
    else
        cache.load_flag = "TACTICAL"
        -- tactical lv 0
        if (api:getMagExtentLevel() == 0) then
            cache.cooldown = param.tactical_cooldown * 1000
            cache.feed = param.tactical_feed * 1000
        -- tactical lv 1
        elseif (api:getMagExtentLevel() == 1) then
            cache.cooldown = param.tactical_xmag_1_cooldown * 1000
            cache.feed = param.tactical_xmag_1_feed * 1000
        -- tactical lv 2 3
        elseif (api:getMagExtentLevel() == 2 or api:getMagExtentLevel() == 3) then
            cache.cooldown = param.tactical_xmag_2_cooldown * 1000
            cache.feed = param.tactical_xmag_2_feed * 1000
        end
    end
    api:cacheScriptData(cache)
    return true
end

function M.tick_reload(api)
    local cache = api:getCachedScriptData()
    -- 缓存由 start_reload 建立。切枪时引擎可能拿着新枪来 tick 上一把枪遗留的换弹状态，
    -- 此时新枪的 start_reload 从未执行过，缓存为空——直接取字段会抛 LuaError 崩客户端。
    if (cache == nil) then
        return NOT_RELOADING, -1
    end
    local cooldown = cache.cooldown
    local feed = cache.feed
    local reload_time = api:getReloadTime()

    if (reload_time < feed) then
        return EMPTY_RELOAD_FEEDING, feed - reload_time
    elseif (reload_time >= feed and reload_time < cooldown) then
        if (not cache.loaded) then
            if (api:isReloadingNeedConsumeAmmo()) then
                if (cache.load_flag == "EMPTY") then
                    api:putAmmoInMagazine(api:consumeAmmoFromPlayer(api:getNeededAmmoAmount())-1)
                    api:setAmmoInBarrel(true)
                elseif (cache.load_flag == "TACTICAL") then
                    api:putAmmoInMagazine(api:consumeAmmoFromPlayer(api:getNeededAmmoAmount()))
                end
            else
                if (cache.load_flag == "EMPTY") then
                    api:putAmmoInMagazine(api:getNeededAmmoAmount()-1)
                    api:setAmmoInBarrel(true)
                elseif (cache.load_flag == "TACTICAL") then
                    api:putAmmoInMagazine(api:getNeededAmmoAmount())
                end
            end
            cache.loaded = true
            api:cacheScriptData(cache)
        end
        return EMPTY_RELOAD_FINISHING, cooldown - reload_time
    elseif (reload_time >= cooldown) then
        return NOT_RELOADING, -1
    end
end

return M