-- Token bucket rate limiter.
-- KEYS[1] = bucket key
-- ARGV[1] = max tokens (bucket capacity)
-- ARGV[2] = refill rate (tokens per second)
-- ARGV[3] = current timestamp (seconds)
-- ARGV[4] = requested tokens
-- Returns: 1=allowed, 0=denied

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Get current bucket state
local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1]) or max_tokens
local last_refill = tonumber(bucket[2]) or now

-- Refill tokens based on elapsed time
local elapsed = now - last_refill
local refill = elapsed * refill_rate
tokens = math.min(max_tokens, tokens + refill)

-- Check if enough tokens
if tokens >= requested then
    -- Deduct tokens
    tokens = tokens - requested
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
    -- Set expiry (2x the time to refill a full bucket)
    redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate) * 2)
    return 1
else
    -- Not enough tokens, update state
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
    redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate) * 2)
    return 0
end
