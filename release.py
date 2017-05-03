#!/usr/bin/env python
#
# Release script for Sheater
#

import os

try:
    from hostage import *
except ImportError:
    print "!! Release library unavailable."
    print "!! Use `pip install hostage` to fix."
    print "!! You will also need an API token in .github.token,"
    print "!!  a .hubrrc config, or `brew install hub` configured."
    print "!! A $GITHUB_TOKEN env variable will also work."
    exit(1)

# TODO possibly github releases, tags, etc.

# verify tests
verify(Execute("lein doo phantom test once")).succeeds(silent=False).orElse(die())

commitHash = Execute("git rev-parse --short HEAD").output().strip()
ghPagesCommit = "Auto-generated deploy of %s" % commitHash

verify(Execute("lein do clean, cljsbuild once min")).succeeds(silent=False).orElse(die())

verify(Execute("rm -rf dist/*")).succeeds().orElse(echoAndDie("Couldn't clean up dist directory"))

# copy into it
verify(Execute("cp -r resources/public/ dist/")).succeeds().orElse(echoAndDie("Couldn't export resources to dist"))

print "Stepping into `dist`..."
os.chdir("dist")

verify(Execute("git add --all")).succeeds()

verify(Execute("git", "commit", "-m", ghPagesCommit)).succeeds()
verify(Execute("git", "push")).succeeds()

print "Done!"

# flake8: noqa
