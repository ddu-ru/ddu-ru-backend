redis.call('SET', KEYS[1], '1', 'PX', ARGV[1])
return redis.call('PEXPIRETIME', KEYS[1])
