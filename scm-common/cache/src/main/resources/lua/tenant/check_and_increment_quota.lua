-- Atomic check-and-increment for daily API quota.
-- KEYS[1] = counter key
-- ARGV[1] = daily limit
-- ARGV[2] = TTL in seconds (until end of day)
-- Returns: 1 if allowed, 0 if quota exceeded

local current = redis.call('GET', KEYS[1])
local count = current and tonumber(current) or 0
if count >= tonumber(ARGV[1]) then
    return 0
end
count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return 1
