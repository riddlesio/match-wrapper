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
    err('engine received: ' + message)
    parts = message.split()

    if not parts:
        return err('Unable to parse line (empty)')

    message_map = {
        'initialize': initialize,
        'start': start,
        'details': details,
        'game': game,
        'bot_ids': do_nothing,
        'configuration': do_nothing,
        'bot': handle_bot,
    }

    message_type = parts.pop(0)

    if message_type not in message_map:
        return err('Unable to parse line')

    message_map[message_type](*parts)

def initialize(*args):
    out('ok')

def start(*args):
    global roundnr
    roundnr = roundnr + 1
    out('bot all send settings timebank 1000')
    out('bot 0 send update game field 0,0,0,1')
    out('bot 0 send update player1 points 0')
    out('bot 0 ask hello')

def handle_bot(bot_id, *args):
    global winner
    if int(bot_id) == 0:
        winner = 0
        end()

def end(*args):
    out('end')

def do_nothing(*args):
    pass

def details(*args):
    out('winner {} round {}'.format(winner, roundnr))

def game(*args):
    out('{{"winner": {}}}'.format(winner))

def out(message):
    sys.stdout.write(message + '\n')
    sys.stdout.flush()

def err(message):
    sys.stderr.write(message + '\n')
    sys.stderr.flush()

if __name__ == '__main__':
    run()
