/**
 * Trigger a puppet build on a specific host, generally to tell it to deploy
 * something from the job that has just been built.
*/

def call(String server) {
    sh """
        kinit -t /opt/jenkins/deploy/ocfdeploy.keytab ocfdeploy \
            ssh "ocfdeploy@${server}" "sudo puppet-trigger --no-daemonize"
    """
}
