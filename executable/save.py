#!/bin/env python

import os
import sys
import subprocess

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

bin = os.path.dirname(os.path.abspath(sys.argv[0]))

if not 'TESTMP_HOME' in os.environ:
    home = os.path.dirname(bin)
    os.environ['TESTMP_HOME'] = home
   
executable = 'java'
if 'JAVA_HOME' in os.environ:
    executable = os.path.join(os.environ['JAVA_HOME'], 'bin', 'java')

testmp_lib = os.path.join(os.environ['TESTMP_HOME'], 'lib')
dependencies = os.listdir(testmp_lib)
dependencies = [os.path.join(testmp_lib, d) for d in dependencies]
dependencies.insert(0, '.')

os.chdir(bin)
command = [executable, '-cp', ':'.join(dependencies), 'SaveLoad', 'save']

if len(sys.argv) > 1:
    command.extend(sys.argv[1:])

print command
try:
    subprocess.call(command)
except:
    pass
