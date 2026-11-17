#!/bin/sh
# starts a server, prints our log lines and errors, stops it. usage: tools/smoke_server.sh 26.3
v=$1; log=/tmp/eco-server-$v.log
pkill -f "fml.startup" 2>/dev/null
(timeout 420 ./gradlew :$v:runServer --console=plain --no-parallel > $log 2>&1 &)
for i in $(seq 1 84); do
  sleep 5
  grep -qE 'Done \(|Failed to start|Crash report|BUILD FAILED|Exception in server tick' $log && break
done
grep -E 'Done \(|ecologies|Ecologies' $log | grep -v 'Got mod coordinates' | head -30
echo "--- errors/warnings:"
grep -E '/ERROR|/WARN' $log | grep -viE 'Ambiguity|OFFLINE|authenticate|hackers|online-mode|refmap|unexpected schema' | head -20
pkill -f "GradleDaemon|GradleWrapperMain|fml.startup"
