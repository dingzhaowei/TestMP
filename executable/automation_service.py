import argparse
import cgi
import logging
import os
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

logger = logging.getLogger('automation_service')
logger.setLevel(logging.DEBUG)

fmt = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh = logging.FileHandler('automation_service.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(fmt)
logger.addHandler(fh)

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
            self.wfile.write(resp.encode('UTF-8'))
        except Exception as e:
            self.send_error(502, str(e))
            traceback.print_exc(file=sys.stderr)

def query(automations):
    logger.debug('query automation: ' + automations)
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
        if automation not in waitings and automation not in runnings:
            logger.debug('launch automation: ' + automation)
            waitings.append(automation)
            condition.notify_all()
        return '1'
    finally:
        condition.release()

def cancel(automation):
    condition.acquire()
    try:
        if automation in runnings:
            logger.debug('cancel automation: ' + automation + ' [terminated]')
            runnings[automation].terminate()
            del runnings[automation]
            condition.notify_all()
        elif automation in waitings:
            logger.debug('cancel automation: ' + automation + '[unregistered]')
            waitings.remove(automation)
            condition.notify_all()
        return '0'
    finally:
        condition.release()

def testng_default():
    java = java_executable()
    if args.jvmargs:
        java = java + ' ' + args.jvmargs
    if not args.classpath:
        return java + ' org.testng.TestNG -methods {c}.{m}'
    classpath = []
    for cp in args.classpath.split(os.path.pathsep):
        classpath.append(cp)
        if os.path.isdir(cp):
            classpath.extend(find_jars(cp))
    classpath = os.path.pathsep.join(classpath)
    return ' '.join([java, '-cp', classpath, 'org.testng.TestNG -methods {c}.{m}'])

def junit_default():
    return None

def maven_default():
    os.chdir(args.basedir)
    return 'mvn -Dtest={c}#{m} -DargLine="%s" test' % args.jvmargs

def java_executable():
    executable = 'java'
    if 'JAVA_HOME' in os.environ:
        executable = os.path.join(os.environ['JAVA_HOME'], 'bin', 'java')
    return executable

def find_jars(basedir):
    jars = []
    for root, dirs, files in os.walk(basedir):
        for name in files:
            if name.endswith('.jar'):
                jars.append(os.path.join(root, name))
    return jars

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    """Handle requests in a separate thread."""
    pass

class AutomationRunner(threading.Thread):

    def run(self):
        while True:
            condition.acquire()
            try:
                if len(runnings) >= args.numproc or not waitings:
                    logger.debug('waiting for tests ready to run')
                    condition.wait()
                    continue
                automation = waitings.pop(0)
                try:
                    classname, methodname = automation.rsplit('.', 1)
                except ValueError:
                    classname, methodname = automation, ''
                command = args.command.format(c=classname, m=methodname)
                logger.debug('run: ' + command)
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
                    logger.debug('no running test to check')
                    condition.wait()
                    continue
                for automation in list(runnings.keys()):
                    retcode = runnings[automation].poll()
                    if retcode != None:
                        del runnings[automation]
                        logger.debug('done: ' + automation + ', retcode=' + str(retcode))
                        condition.notify_all()
            finally:
                condition.release()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Handle requests of running test automation')

    parser.add_argument('-c', '--command', required=True, help='command to run a test parameterized as {a}')
    parser.add_argument('-p', '--port', type=int, default=8888, help='listening port of the service')
    parser.add_argument('-n', '--numproc', type=int, default=1, help='max number of automation runs at the same time')

    parser.add_argument('-x', '--classpath', default='.', help='classpath for running (testng/junit)_default command')
    parser.add_argument('-d', '--basedir', default='.', help='basedir for running maven_default command')
    parser.add_argument('-a', '--jvmargs', default=None, help='jvm arguments passed to the test run')

    args = parser.parse_args()

    if args.command == 'testng_default':
        args.command = testng_default()
    elif args.command == 'junit_default':
        args.command = junit_default()
    elif args.command == 'maven_default':
        args.command = maven_default()

    runner = AutomationRunner()
    runner.setDaemon(True)
    runner.start()

    checker = AutomationChecker()
    checker.setDaemon(True)
    checker.start()

    server = ThreadedHTTPServer(('127.0.0.1', args.port), Handler)
    print('Starting server, use <Ctrl-C> to stop')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print()