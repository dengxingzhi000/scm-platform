-- Atomically acquire a distributed lock.
-- KEYS[1] = lock key
-- ARGV[1] = lock value (owner token)
-- ARGV[2] = TTL in milliseconds
-- Returns: 1 if acquired, 0 otherwise

if redis.call('EXISTS', KEYS[1]) == 0 then
    redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
    return 1
end
return 0
