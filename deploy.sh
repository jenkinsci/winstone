#!/bin/sh
mvn -DupdateReleaseInfo=true clean source:jar deploy
git push origin master
