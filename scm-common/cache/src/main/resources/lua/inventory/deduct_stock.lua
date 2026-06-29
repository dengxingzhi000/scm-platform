-- Atomically deduct stock. Prevents overselling.
-- KEYS[1] = stock key
-- ARGV[1] = quantity to deduct
-- Returns: 1 = success, -1 = stock not found, -2 = insufficient stock

local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1
end
if tonumber(stock) < tonumber(ARGV[1]) then
    return -2
end
redis.call('DECRBY', KEYS[1], ARGV[1])
return 1
