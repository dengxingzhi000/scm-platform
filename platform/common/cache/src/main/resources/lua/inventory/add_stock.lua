-- Atomically add stock. Creates key if not exists.
-- KEYS[1] = stock key
-- ARGV[1] = quantity to add
-- Returns: new stock count after addition

local stock = redis.call('GET', KEYS[1])
if not stock then
    redis.call('SET', KEYS[1], ARGV[1])
    return tonumber(ARGV[1])
else
    return redis.call('INCRBY', KEYS[1], ARGV[1])
end
