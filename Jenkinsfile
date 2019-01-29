#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')
def utils = new io.fabric8.Utils()

releaseNode {
  try {
    checkout scm
    readTrusted 'release.groovy'

    if (utils.isCI()) {

      echo 'CI is not handled by pipelines yet'

    } else if (utils.isCD()) {
      sh "git remote set-url origin git@github.com:fabric8io/mockwebserver.git"

      def pipeline = load 'release.groovy'
      def stagedProject

      stage('Stage') {
        stagedProject = pipeline.stage()
      }

      stage('Promote') {
        pipeline.release(stagedProject)
      }

      stage('Promote YAMLs') {
        pipeline.promoteYamls(stagedProject[1])
      }
    }
  } catch (err) {
    hubot room: 'release', message: "${env.JOB_NAME} failed: ${err}"
    error "${err}"
  }
}
