#!/bin/bash
# usage: tools/scenario.sh <version> <script>
# one command per line, "#wait N" to pause. log goes to /tmp/eco-scenario-<version>.log
v=$1; script=$2; log=/tmp/eco-scenario-$v.log
pkill -f "GradleDaemon|GradleWrapperMain|fml.startup" 2>/dev/null; sleep 1
[ -n "$KEEP" ] || rm -rf run/$v/server/world
touch run/$v/server/server.properties; sed -i "/pause-when-empty-seconds/d" run/$v/server/server.properties; echo "pause-when-empty-seconds=0" >> run/$v/server/server.properties
feed() {
  # wait for the server to finish starting
  for i in $(seq 1 120); do grep -q 'Done (' $log 2>/dev/null && break; sleep 2; done
  sleep 2
  while IFS= read -r line; do
    case "$line" in
      "#wait "*) sleep "${line#\#wait }" ;;
      ""|"#"*) ;;
      *) echo "$line"; sleep 0.3 ;;
    esac
  done < "$script"
  echo stop
  sleep 20
}
feed | timeout 600 ./gradlew :$v:runServer -Pecologies.stdin ${GRADLE_EXTRA:-} --console=plain --no-parallel > $log 2>&1
grep -E 'System chat|Survey|Random-ticked|\[Server\]|\[Not Secure\]|filled|Killed|has the following|ERROR|Exception' $log | grep -v 'DEBUG' | sed 's/^.*\]: //' | head -60
