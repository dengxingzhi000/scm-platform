-- Atomically deduct stock for multiple keys.
-- All keys are validated first; nothing is deducted unless every item is sufficient.
-- KEYS[i] = stock key of item i
-- ARGV[i] = quantity to deduct for item i
-- Returns:
--   1          = success (all items deducted)
--   -1         = a stock key does not exist
--   -(i + 1)   = item i has insufficient stock (i is 1-based)

local count = #KEYS
for i = 1, count do
    local stock = redis.call('GET', KEYS[i])
    if not stock then
        return -1
    end
    if tonumber(stock) < tonumber(ARGV[i]) then
        return -(i + 1)
    end
end

for i = 1, count do
    redis.call('DECRBY', KEYS[i], ARGV[i])
end

return 1
