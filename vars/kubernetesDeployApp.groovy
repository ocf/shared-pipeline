/**
 * Run our kubernetes-deploy script on an app/service, generally the latest
 * version of the job that has just been built.
*/


def call(String app, String version) {
    ansiColor('xterm') {
        withKubeConfig([credentialsId: 'kubernetes-deploy-token',
                        serverUrl: 'https://kubernetes.ocf.berkeley.edu:6443'
        ]) {
            sh """
                docker run \
                -e REVISION=${GIT_COMMIT} \
                -v "$KUBECONFIG":/kubeconfig:ro \
                -v "$(pwd)"/kubernetes:/input:ro \
                -t docker.ocf.berkeley.edu/kubernetes-deploy \
                "$app" "$version"
            """
        }
    }
}

// vim: ft=groovy
