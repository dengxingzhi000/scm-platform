-- Increment a key and set expiry (in milliseconds).
-- KEYS[1] = key to increment
-- ARGV[1] = expiry in milliseconds
-- Returns: new value after increment

local current = redis.call('INCR', KEYS[1])
redis.call('PEXPIRE', KEYS[1], ARGV[1])
return current
