-- Sliding window rate limiter (Sorted Set based).
-- KEYS[1] = rate limit key
-- ARGV[1] = max requests per window
-- ARGV[2] = window size in milliseconds
-- ARGV[3] = current timestamp in milliseconds
-- ARGV[4] = unique request id (e.g. UUID)
-- Returns: 1=allowed, 0=denied

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local request_id = ARGV[4]

-- Remove expired requests outside the window
local window_start = now - window
redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)

-- Count current requests in window
local count = redis.call('ZCARD', key)

if count < limit then
    -- Allowed: add current request
    redis.call('ZADD', key, now, request_id)
    -- Set key expiry to window size (convert ms to seconds)
    redis.call('PEXPIRE', key, window)
    return 1
else
    -- Denied
    return 0
end
