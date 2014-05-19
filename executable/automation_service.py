import argparse
import cgi
import shlex
import subprocess
import sys
import threading
import traceback

from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

# TestMP (Test Management Platform)
# Copyright 2013 and beyond, Zhaowei Ding.
# 
# TestMP is free software; you can redistribute it and/or modify it
# under the terms of the MIT License (MIT).
# 
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.

waitings = []
runnings = {}
condition = threading.Condition(threading.Lock())

class Handler(BaseHTTPRequestHandler):

    def do_POST(self):
        try:
            e = {'REQUEST_METHOD':'POST', 'CONTENT_TYPE': 'text/plain',}
            form = cgi.FieldStorage(fp=self.rfile, headers=self.headers, environ=e)
            automation = form['automation'].value
            action = form['action'].value
            resp = globals()[action](automation)
            self.send_response(200)
            self.end_headers()
            self.wfile.write(resp)
        except Exception as e:
            self.send_error(502, str(e))

def query(automations):
    automations, resp = automations.split(','), ''
    condition.acquire()
    try:
        for automation in automations:
            if automation in runnings or automation in waitings:
                resp += '1'
            else:
                resp += '0'
        return resp
    finally:
        condition.release()

def launch(automation):
    condition.acquire()
    try:
        if automation in waitings or automation in runnings:
            return '1'
        waitings.add(automation)
        condition.notify_all()
    finally:
        condition.release()

def cancel(automation):
    condition.acquire()
    try:
        if automation in runnings:
            runnings[automation].terminate()
            del runnings[automation]
            condition.notify_all()
        elif automation in waitings:
            waitings.remove(automation)
            condition.notify_all()
        return '0'
    finally:
        condition.release()

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    """Handle requests in a separate thread."""
    pass

class AutomationRunner(threading.Thread):

    def run(self):
        while True:
            condition.acquire()
            try:
                if len(runnings) >= args.numproc or not waitings:
                    condition.wait()
                    continue
                automation = waitings.pop(0)
                command = args.command.format(automation)
                p = subprocess.Popen(shlex.split(command))
                runnings[automation] = p
                condition.notify_all()
            except:
                traceback.print_exc(file=sys.stderr)
            finally:
                condition.release()

class AutomationChecker(threading.Thread):

    def run(self):
        while True:
            condition.acquire()
            try:
                if not runnings:
                    condition.wait()
                    continue
                for automation in runnings.keys():
                    retcode = runnings[automation].poll()
                    if retcode != None:
                        del runnings[automation]
                        print(automation, retcode)
            finally:
                condition.release()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Handle requests of running test automation')
    parser.add_argument('-p', '--port', type=int, default=10085, help='listening port of the service')
    parser.add_argument('-n', '--numproc', type=int, default=1, help='max number of automation runs at the same time')
    parser.add_argument('-c', '--command', required=True, help='command to run a test parameterized ${automation}')
    args = parser.parse_args()

    runner = AutomationRunner()
    runner.setDaemon(True)
    runner.start()

    checker = AutomationChecker()
    checker.setDaemon(True)
    checker.start()

    server = ThreadedHTTPServer(('localhost', args.port), Handler)
    print('Starting server, use <Ctrl-C> to stop')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print()