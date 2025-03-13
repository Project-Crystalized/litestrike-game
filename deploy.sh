#!/bin/sh

./gradlew build

scp build/libs/* cry@crystalized.cc:~/servers/litestrike-portside/plugins/
scp build/libs/* cry@crystalized.cc:~/servers/litestrike-mall/plugins/
scp build/libs/* cry@crystalized.cc:~/servers/litestrike-zero/plugins/
scp build/libs/* cry@crystalized.cc:~/servers/litestrike-crusaders/plugins/
scp build/libs/* cry@crystalized.cc:~/servers/litestrike-dust2/plugins/
scp build/libs/* cry@crystalized.cc:~/servers/litestrike-plaza/plugins/


