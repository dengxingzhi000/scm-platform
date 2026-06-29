-- Renew a distributed lock TTL only if the caller owns it.
-- KEYS[1] = lock key
-- ARGV[1] = lock value (owner token)
-- ARGV[2] = new TTL in milliseconds
-- Returns: 1 if renewed, 0 otherwise

if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('PEXPIRE', KEYS[1], ARGV[2])
else
    return 0
end
