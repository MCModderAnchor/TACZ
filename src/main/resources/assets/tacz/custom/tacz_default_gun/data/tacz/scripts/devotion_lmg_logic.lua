local M = {}
local acceleration_max = 7  -- 最大加速多少次
local acceleration_ratio = 0.08  -- 每次加速的百分比

function M.shoot(api)
    -- 调用射击逻辑
    api:shootOnce(api:isShootingNeedConsumeAmmo())
    -- 从脚本缓存中获取已经加速的次数，
    local cache = api:getCachedScriptData()
    if (cache == nil) then
        cache = {
            acceleration_count = 0 -- 当前加速了几次，其实就是连射次数
        }
    end
    -- 获取上次射击的 timestamp（系统时间，单位毫秒）
    local last_shoot_timestamp = api:getLastShootTimestamp()
    -- 获取当前系统时间
    local current_timestamp = api:getCurrentTimestamp()
    -- 获取枪械的射击间隔，单位毫秒。用于判断是否在连射，也用于调整射击间隔
    local shoot_interval = api:getShootInterval()
    -- 判断是否在连射，给 2 tick 宽容时间
    if (current_timestamp - last_shoot_timestamp < shoot_interval + 100) then
        -- 正在连射，连射次数 +1
        if (cache.acceleration_count < acceleration_max) then
            cache.acceleration_count = cache.acceleration_count + 1
        end
        -- 根据连射次数调整射击间隔
        local total_ratio = cache.acceleration_count * acceleration_ratio
        api:adjustShootInterval(-total_ratio * shoot_interval)
    else
        -- 没有在连射，需要重置连射次数
        cache.acceleration_count = 0
        -- 没有加速，不需要调整射击间隔
    end
    -- 写回脚本缓存
    api:cacheScriptData(cache)
end

return M