/**
 * A standard pipeline for an OCF docker-based service build
*/


def call(Map pipelineParams = [:]) {
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
                        repo_name_tokens = env.JOB_NAME.split('/')
                        repo_name = repo_name_tokens.length < 2 ? env.JOB_NAME : repo_name_tokens[repo_name_tokens.length - 2]
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
                            sh 'make -q cook-image || if [ "$?" -ne "2" ]; then make cook-image; fi'
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
                    sh 'make -q push-image || if [ "$?" -ne "2" ]; then make push-image; fi'
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
                ircNotification()
            }
        }
    }
}

// vim: ft=groovy
