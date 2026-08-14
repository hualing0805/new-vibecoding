UPDATE users
SET password_hash = '{noop}ThreadMind@123'
WHERE username IN ('demo', 'beiqiao', 'lin')
  AND password_hash = '{noop}threadmind123';
