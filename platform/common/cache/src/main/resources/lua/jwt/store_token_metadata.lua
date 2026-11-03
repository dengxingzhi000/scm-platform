-- Store JWT token metadata atomically.
-- KEYS[1] = userTokensHash (user device → token mapping)
-- KEYS[2] = fingerprintKey (per-token metadata)
-- ARGV[1] = deviceId, ARGV[2] = token, ARGV[3] = ttl (seconds)
-- ARGV[4] = userId, ARGV[5] = ipAddress, ARGV[6] = issueTime
-- Returns: 1 on success

redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
redis.call('EXPIRE', KEYS[1], ARGV[3])
redis.call('HMSET', KEYS[2], 'userId', ARGV[4], 'deviceId', ARGV[1], 'ipAddress', ARGV[5], 'issueTime', ARGV[6])
redis.call('EXPIRE', KEYS[2], ARGV[3])
return 1
