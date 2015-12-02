#!/bin/bash
RUN_WRAPPER=$(dirname $0)/../run_wrapper.sh
TIMEBANKMAX="1000"
TIMEPERMOVE="500"
MAXTIMEOUTS="2"

$RUN_WRAPPER $TIMEBANKMAX $TIMEPERMOVE $MAXTIMEOUTS "python3 $(dirname $0)/hello_engine.py" "python3 $(dirname $0)/hello_bot.py"