local M = {}
local track_line_top = 0
local STATIC_TRACK = -1
local BOLT_CAUGHT_TRACK = -1
local SAFETY_TRACK = -1
local MOVE_ANIMATION_TRACK = -1
local ADS_TRACK = -1
local MAIN_TRACK = -1

function M.initialize(context)

end

function M.exit(context)

end

local idle = {

}

function M:states()
    local stateTable = {}
    stateTable[1] = {}
    return stateTable
end

return M