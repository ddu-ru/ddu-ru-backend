-- The response and Redis share the same absolute expiration (epoch milliseconds).
redis.call('SET', KEYS[1], ARGV[1], 'PXAT', ARGV[2])
return 1
