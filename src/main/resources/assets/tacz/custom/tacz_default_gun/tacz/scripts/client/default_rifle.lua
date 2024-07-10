local M = {}

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