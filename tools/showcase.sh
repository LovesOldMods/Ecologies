#!/bin/bash
# usage: tools/showcase.sh <version>  (screenshot ends up in /tmp/showcase-<version>.png)
v=$1
script=tools/scenarios/showcase.txt
if [ "$v" != "1.21.1" ]; then
  sed -e 's/gamerule doDaylightCycle false/gamerule advance_time false/' -e 's/gamerule doMobSpawning false/gamerule spawn_mobs false/' \
      -e 's/gamerule doWeatherCycle false/gamerule advance_weather false/' -e 's/gamerule randomTickSpeed 0/gamerule random_tick_speed 0/' -e 's/gamerule spawnRadius 0/gamerule respawn_radius 0/' \
      $script > /tmp/showcase-$v.txt
  script=/tmp/showcase-$v.txt
fi
tools/scenario.sh $v $script > /tmp/showcase-server-$v.txt 2>&1
mkdir -p run/$v/client/saves
rm -rf run/$v/client/saves/showcase
cp -r run/$v/server/world run/$v/client/saves/showcase
# skip the first-launch screens
mkdir -p run/$v/client
grep -q onboardAccessibility run/$v/client/options.txt 2>/dev/null || printf 'onboardAccessibility:false\ntutorialStep:none\njoinedFirstServer:true\nskipMultiplayerWarning:true\nfov:0.0\nrenderDistance:6\n' >> run/$v/client/options.txt
pkill -f "GradleDaemon|fml.startup" 2>/dev/null; sleep 1
Xvfb :99 +extension GLX -screen 0 1280x720x24+32 >/dev/null 2>&1 &
xpid=$!
export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1
(timeout 600 ./gradlew :$v:runClient -Pecologies.world=showcase --console=plain --no-parallel > /tmp/client-$v.log 2>&1 &)
for i in $(seq 1 100); do
  sleep 5
  grep -qE 'Saving and pausing|joined the game|Crash|BUILD FAILED' /tmp/client-$v.log && break
done
sleep ${WAIT:-40}
import -window root /tmp/showcase-$v.png
pkill -f "GradleDaemon|fml.startup"; kill $xpid
grep -E 'joined the game|Crash|ERROR' /tmp/client-$v.log | head -5
