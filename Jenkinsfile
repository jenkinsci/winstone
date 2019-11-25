#!/usr/bin/env groovy

/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])


/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
Map branches = [:]
['maven', 'maven-windows'].each {label ->
    branches[label] = {
        node(label) {
            timestamps {
                stage('Checkout') {
                    checkout scm
                }

                stage('Build') {
                    timeout(30) {
                        infra.runMaven(['clean', 'install', '-Dset.changelist', '-Dmaven.test.failure.ignore=true'], 8, null, null, false)
                    }
                }

                stage('Archive') {
                    /* Archive the test results */
                    junit '**/target/surefire-reports/TEST-*.xml'

                    if (label == 'maven') {
                        infra.prepareToPublishIncrementals()
                    }
                }
            }
        }
    }
}

/* Execute our platforms in parallel */
parallel(branches)

infra.maybePublishIncrementals()
