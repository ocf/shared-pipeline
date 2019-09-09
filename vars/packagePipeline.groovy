/**
 * A standard pipeline for an OCF package build
*/

Map parallelBuilds(Collection dists) {
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


def call(Map pipelineParams = [:]) {
    pipeline {
        // TODO: Make this cleaner: https://issues.jenkins-ci.org/browse/JENKINS-42643
        triggers {
            upstream(
                upstreamProjects: (env.BRANCH_NAME == 'master' ? pipelineParams.upstreamProjects?.join(',') : ''),
                threshold: hudson.model.Result.SUCCESS,
            )
        }

        agent {
            label 'slave'
        }

        options {
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
                    // Get a coveralls or codecov token from the parameters,
                    // but don't error if it doesn't exist (not all projects
                    // are using coveralls or codecov)
                    COVERALLS_REPO_TOKEN = credentials("${pipelineParams.getOrDefault('coverallsToken', 'default')}")
                    CODECOV_REPO_TOKEN = credentials("${pipelineParams.getOrDefault('codecovToken', 'default')}")
                }
                steps {
                    sh 'make -q test || if [ "$?" -ne "2" ]; then make test; fi'
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
                        def builds = parallelBuilds(pipelineParams.dists)
                        parallel builds
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
