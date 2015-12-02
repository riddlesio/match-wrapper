#!/usr/bin/env python3

import sys

winner = None
roundnr = 0

def run():
    while not sys.stdin.closed:
        try:
            rawline = sys.stdin.readline()
            line = rawline.strip()
            handle_message(line)
        except EOFError:
            err('EOF')
            return

def handle_message(message):
    err('Message received: ' + message)
    parts = message.split()
    if not parts:
        err('Unable to parse line (empty)') 
    elif parts[0] == 'initialize':
        initialize()
    elif parts[0] == 'start':
        start()
    elif parts[0] == 'details':
        details()
    elif parts[0] == 'bot_ids':
        # set bot ids
        pass
    elif parts[0] == 'bot':
        handle_bot(int(parts[1]))
    else:
        err('Unable to parse line')

def initialize():
    out('ok')

def start():
    global roundnr
    roundnr = roundnr + 1
    out('bot all send settings timebank 1000')
    out('bot 0 send update game field 0,0,0,1')
    out('bot 0 send update player1 points 0')
    out('bot 0 ask hello')

def handle_bot(bot_id):
    global winner
    if bot_id == 0:
        winner = 0
        end()

def end():
    out('end')

def details():
    out('winner {} round {}'.format(winner, roundnr))

def out(message):
    sys.stdout.write(message + '\n')
    sys.stdout.flush()

def err(message):
    sys.stderr.write(message + '\n')
    sys.stderr.flush()

if __name__ == '__main__':
    run()
