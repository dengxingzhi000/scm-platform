-- Add a JWT token to the blacklist atomically.
-- KEYS[1] = blacklistKey
-- ARGV[1] = revokeTime, ARGV[2] = reason, ARGV[3] = userId, ARGV[4] = ttl (seconds)
-- Returns: 1 on success

redis.call('HMSET', KEYS[1], 'revokeTime', ARGV[1], 'reason', ARGV[2], 'userId', ARGV[3])
redis.call('EXPIRE', KEYS[1], ARGV[4])
return 1
