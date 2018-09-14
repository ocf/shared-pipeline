/**
 * A standard pipeline for an OCF package build
*/

def parallelBuilds(Map dists) {
    dists.collectEntries { dist ->
        [dist, {
            stage("build-${dist}") {
                sh 'make clean'
                sh "make package_${dist}"
                archiveArtifacts artifacts: "dist_${dist}/*"
            }
        }]
    }
}


def call(Map pipelineParams) {
    pipeline {
        agent {
            label 'slave'
        }

        options {
            ansiColor('xterm')
            timeout(time: 1, unit: 'HOURS')
            timestamps()
        }

        stages {
            stage('check-gh-trust') {
                steps {
                    checkGitHubAccess()
                }
            }

            stage('test') {
                environment {
                    // Get a coverall token from the parameters, but don't
                    // error if it doesn't exist (not all projects are using
                    // coveralls)
                    COVERALLS_REPO_TOKEN = pipelineParams.get('coverallsToken')
                }
                steps {
                    sh 'make test'
                }
            }

            stage('push-to-pypi') {
                when {
                    branch 'master'
                    equals expected: true, actual: pipelineParams.get('pypiRelease')
                }
                agent {
                    label 'deploy'
                }
                steps {
                    sh 'make release-pypi'
                }
            }

            stage('parallel-builds') {
                steps {
                    script {
                        parallel parallelBuilds(pipelineParams.dists)
                    }
                }
            }

            stage('upload-packages') {
                when {
                    branch 'master'
                }
                agent {
                    label 'deploy'
                }
                steps {
                    script {
                        // Upload packages in series instead of in parallel to avoid a race
                        // condition with a lock file on the package repo
                        for(dist in pipelineParams.dists) {
                            stage("upload-${dist}") {
                                uploadChanges(dist, "dist_${dist}/*.changes")
                            }
                        }
                    }
                }
            }
        }

        post {
            failure {
                emailNotification()
            }
            always {
                node(label: 'slave') {
                    ircNotification()
                }
            }
        }
    }
}

// vim: ft=groovy
