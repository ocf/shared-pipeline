/**
 * Run our kubernetes-deploy script on an app/service, generally the latest
 * version of the job that has just been built.
*/


def call(String app, String version) {
    ansiColor('gnome-terminal') {
        withKubeConfig([credentialsId: 'kubernetes-deploy-token',
                        serverUrl: 'https://kubernetes.ocf.berkeley.edu:6443'
        ]) {
            cwd = sh(script: 'pwd', returnStdout: true).trim()
            sh """
                docker run \
                -e REVISION=${GIT_COMMIT} \
                -v "${KUBECONFIG}":/kubeconfig:ro \
                -v "$cwd"/kubernetes:/input:ro \
                docker.ocf.berkeley.edu/kubernetes-deploy \
                "$app" "$version"
            """
        }
    }
}

// vim: ft=groovy
