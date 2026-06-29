-- Atomically check if account is locked or max attempts reached, then lock if needed.
-- KEYS[1] = lock key
-- KEYS[2] = attempts key
-- ARGV[1] = max attempts threshold
-- ARGV[2] = lock value (e.g. timestamp)
-- ARGV[3] = lock expiry in milliseconds
-- Returns: true if locked, false otherwise

if redis.call('EXISTS', KEYS[1]) == 1 then
    return true
end

local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')
if attempts >= tonumber(ARGV[1]) then
    redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
    return true
end

return false
