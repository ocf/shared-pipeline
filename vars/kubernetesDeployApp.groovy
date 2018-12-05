/**
 * Make the Kubernetes namespace for an app, and deploy it with the given
 * version.
*/

import groovy.json.JsonOutput

def call(String app, String version) {
    def namespace = "app-$app"
    def json = JsonOutput.toJson([
        apiVersion: 'v1',
        kind: 'Namespace',
        metadata: [name: namespace]
    ])

    File namespaceFile = File.createTempFile('namespace', '.tmp')
    namespaceFile.write json
    namespaceFile.deleteOnExit()

    withKubeConfig([credentialsId: 'kubernetes-deploy-token',
                    serverUrl: 'https://hozer-75.ocf.berkeley.edu:6443'
                    ]) {
        sh "kubectl apply -f $namespaceFile.absolutePath"
        sh """
            kubernetes-deploy $namespace k8s --template-dir kubernetes \
                --bindings=version=${version}
        """
    }
}
