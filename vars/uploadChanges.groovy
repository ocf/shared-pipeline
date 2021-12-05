/**
 * Upload the output of the job that just ran as a package to the OCF apt repo.
*/

def call(String dist, String changesPath = '*.changes') {
    def job = JOB_NAME.replace('/', '/job/')

    def allowedDists = ['stretch', 'buster', 'bullseye', 'bookworm']
    if (!allowedDists.contains(dist)) {
        throw new Exception("Invalid distribution, must be in ${allowedDists}")
    }

    sh """
        wget -q -O 'archive.zip' \
            "https://jenkins.ocf.berkeley.edu/job/${job}/${BUILD_NUMBER}/artifact/*zip*/archive.zip"

        kinit -t /opt/jenkins/deploy/ocfdeploy.keytab ocfdeploy \
            ssh ocfdeploy@apt "sudo -u ocfapt /opt/apt/bin/include-changes-from-stdin ${dist} ${changesPath}" \
                < archive.zip
    """
}
