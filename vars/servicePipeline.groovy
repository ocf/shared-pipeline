/**
 * A standard pipeline for an OCF docker-based service build
*/


def call(Map pipelineParams) {
    String sha, version

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

            stage('set-version') {
                steps {
                    script {
                        sha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        version = "${new Date().format("yyyy-MM-dd-'T'HH-mm-ss")}-git${sha}"
                        repo_name = env.JOB_NAME.split('/')[0]
                    }
                }
            }

            stage('parallel-test-cook') {
                environment {
                    DOCKER_REPO = 'docker-push.ocf.berkeley.edu/'
                    DOCKER_REVISION = "${version}"
                }
                parallel {
                    stage('test') {
                        environment {
                            // Get a coveralls or codecov token from the
                            // parameters, but don't error if it doesn't exist
                            // (not all projects are using coveralls or codecov)
                            COVERALLS_REPO_TOKEN = credentials("${pipelineParams.getOrDefault('coverallsToken', 'default')}")
                            CODECOV_REPO_TOKEN = credentials("${pipelineParams.getOrDefault('codecovToken', 'default')}")
                        }
                        steps {
                            script {
                                sh 'make -q test || if [ "$?" -ne "2" ]; then make test; fi'
                            }
                        }
                    }

                    stage('cook-image') {
                        steps {
                            sh 'make cook-image'
                        }
                    }
                }
            }

            stage('push-to-registry') {
                environment {
                    DOCKER_REPO = 'docker-push.ocf.berkeley.edu/'
                    DOCKER_REVISION = "${version}"
                }
                when {
                    branch 'master'
                }
                agent {
                    label 'deploy'
                }
                steps {
                    sh 'make push-image'
                }
            }

            stage('deploy-to-prod') {
                when {
                    branch 'master'
                }
                agent {
                    label 'deploy'
                }
                steps {
                    script {
                        // TODO: Make these deploy and roll back together!
                        // TODO: Also try to parallelize these if possible?
                        pipelineParams.deployTargets.each {
                            marathonDeployApp(it, version)
                        }

                        if (fileExists('kubernetes')) {
                            kubernetesDeployApp(repo_name, version)
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
