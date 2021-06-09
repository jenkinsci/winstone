#!/usr/bin/env groovy

/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])


/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
Map branches = [:]
['maven'/* TODO broken , 'maven-windows'*/].each {label ->
    branches[label] = {
        node(label) {
            stage('Checkout') {
                checkout scm
            }

            def settingsXml = "${pwd tmp: true}/settings-azure.xml"
            def ok = infra.retrieveMavenSettingsFile(settingsXml)
            assert ok
            withEnv(["MAVEN_SETTINGS=$settingsXml"]) {
                stage('Build') {
                    timeout(30) {
                        infra.runMaven(["-Dset.changelist", "-Dmaven.test.failure.ignore", "install javadoc:javadoc"])
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
