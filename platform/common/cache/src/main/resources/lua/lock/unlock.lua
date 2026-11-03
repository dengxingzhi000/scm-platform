-- Release a distributed lock only if the caller owns it.
-- KEYS[1] = lock key
-- ARGV[1] = lock value (owner token)
-- Returns: 1 if deleted, 0 otherwise

if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
else
    return 0
end
