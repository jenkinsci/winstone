#!/usr/bin/env groovy

/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])


/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
List platforms = ['linux', 'windows']
Map branches = [:]

for (int i = 0; i < platforms.size(); ++i) {
    String label = platforms[i]
    branches[label] = {
        node(label) {
            timestamps {
                stage('Checkout') {
                    checkout scm
                }

                stage('Build') {
                    timeout(30) {
                        infra.runMaven(['clean', 'install', '-Dmaven.test.failure.ignore=true'])
                    }
                }

                stage('Archive') {
                    /* Archive the test results */
                    junit '**/target/surefire-reports/TEST-*.xml'

                    /* Archive the build artifacts */
                    archiveArtifacts artifacts: 'target/**/*.jar'
                }
            }
        }
    }
}

/* Execute our platforms in parallel */
parallel(branches)
