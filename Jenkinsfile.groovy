library identifier: 'mylibs@main',
    //'master' refers to a valid git-ref
    //'mylibraryname' can be any name you like
    retriever: modernSCM([
      $class: 'GitSCMSource',
      credentialsId: 'fcb6251b-a639-43c8-a009-ca68c4571bf1',
      remote: 'https://github.com/xsoloking/shared-libs-demo.git'
])

properties([
    parameters([
        string(defaultValue: '30', description: '', name: 'TASK_TIMEOUT', trim: false),
        string(defaultValue: 'regcred', description: '', name: 'IMAGE_PULL_SECRETS', trim: false),
        string(defaultValue: 'cloudbees/java-build-tools:latest', description: '', name: 'MAIN_IMAGE_URL', trim: false),
        string(defaultValue: 'pvc-ws', description: '', name: 'PVC_WORKSPACE', trim: false),
        string(defaultValue: 'fcb6251b-a639-43c8-a009-ca68c4571bf1', description: '', name: 'CREDENTIALS_id', trim: false),
        string(defaultValue: '*/dev', description: '', name: 'GIT_REVISION', trim: false),
        string(defaultValue: 'http://192.168.48.2:8888/yusys_devops/devops_cicd.git', description: '', name: 'GIT_URL', trim: false)
        ])
])

def imagePullSecrets = params.IMAGE_PULL_SECRETS
def persistentVolumeClaim = params.PVC_WORKSPACE
def mainImage = params.MAIN_IMAGE_URL
def mTimeout = params.TASK_TIMEOUT

podTemplate(
    containers: [
        containerTemplate(
            name: 'jnlp', 
            image: '192.168.48.2:5002/jenkins/jnlp-slave:4.7-1-jdk11', 
            alwaysPullImage: true,
            ttyEnabled: true),
        containerTemplate(
            name: 'main', 
            image: mainImage, 
            alwaysPullImage: true,
            ttyEnabled: true, 
            privileged: true,
            command: 'cat')
        ],
    volumes: [
        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'), 
        // persistentVolumeClaim(claimName: 'maven-cache', mountPath: '/root/.m2', readOnly: false), 
        // persistentVolumeClaim(claimName: 'npm-cache', mountPath: '/root/.npm', readOnly: false), 
        // persistentVolumeClaim(claimName: 'yarn-cache', mountPath: '/usr/local/share/.cache/yarn', readOnly: false)
        ], 
    imagePullSecrets: [imagePullSecrets],
    nodeSelector: 'kubernetes.io/hostname=node13', 
    showRawYaml: true, 
    workingDir: '/home/jenkins/agent/workspace/ws',
    workspaceVolume: persistentVolumeClaimWorkspaceVolume(claimName: persistentVolumeClaim, readOnly: false)
    )
{
    node(POD_LABEL) {
        timestamps {
            timeout(time: mTimeout, unit: 'MINUTES') {
                stage('Demo') {
                    container('main') {
                        gitCheckout subDir: "src", branch: "${GIT_REVISION}", credentialsId: "${CREDENTIALS_id}", url: "${GIT_URL}"
                        sh '''
                        df -h
                        pwd
                        ls -al
                        '''
                    }
                }
            }
        }
    }
}
