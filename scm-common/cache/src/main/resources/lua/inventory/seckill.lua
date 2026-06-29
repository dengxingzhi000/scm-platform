-- Atomic seckill: check stock + deduct + prevent duplicate purchase.
-- KEYS[1] = stock key
-- KEYS[2] = user purchased key (prevent duplicate)
-- ARGV[1] = quantity to deduct
-- ARGV[2] = user id
-- ARGV[3] = expire seconds (for purchased key)
-- Returns: 1=success, -1=stock not found, -2=insufficient stock, -3=already purchased

-- Check if user already purchased
if redis.call('EXISTS', KEYS[2]) == 1 then
    return -3
end

-- Check stock exists
local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1
end

-- Check stock sufficient
if tonumber(stock) < tonumber(ARGV[1]) then
    return -2
end

-- Deduct stock
redis.call('DECRBY', KEYS[1], ARGV[1])

-- Mark user as purchased
redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])

return 1
