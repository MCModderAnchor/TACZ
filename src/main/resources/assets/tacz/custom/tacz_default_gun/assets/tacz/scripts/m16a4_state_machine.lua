-- 脚本的位置是 "{命名空间}:{路径}"，那么 require 的格式为 "{命名空间}_{路径}"
-- 注意！require 取得的内容不应该被修改，应仅调用
local default = require("tacz_default_state_machine")
local GUN_KICK_TRACK_LINE = default.GUN_KICK_TRACK_LINE

local function isEaster()
    local flag = math.random(1, 1000)
    -- 改下面一行的数字决定概率，概率是千分之 x
    if (flag <= 100) then
        return true
    end
    return false
end

local handle_state = {
    attachment = "1",
    easter = false
}

function handle_state.entry(this, context)
    handle_state.attachment = context:getAttachment("GRIP")
end

function handle_state.update(this, context)
    if (handle_state.attachment ~= context:getAttachment("GRIP")) then
        handle_state.attachment = context:getAttachment("GRIP")
        handle_state.easter = isEaster()
    end
    local track = context:findIdleTrack(GUN_KICK_TRACK_LINE, false)
    if (context:getAttachment("SCOPE") == "tacz:scope_acog_ta31") then

        if ((context:getAttachment("GRIP") ~= "tacz:empty") and handle_state.easter) then
            context:runAnimation("handle_on", track, true, PLAY_ONCE_STOP, 0)
        else
            context:runAnimation("handle_off", track, true, PLAY_ONCE_STOP, 0)
        end
    else
    context:runAnimation("handle_off", track, true, PLAY_ONCE_STOP, 0)
    end
end


-- 用元表的方式继承默认状态机的属性
local M = setmetatable({
    handle_state = handle_state
}, {__index = default})
function M:initialize(context)
    default.initialize(self, context)
end
-- 继承默认状态机需要重新初始化状态
function M:states()
    return {
        self.handle_state,
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
-- 导出状态机
return M