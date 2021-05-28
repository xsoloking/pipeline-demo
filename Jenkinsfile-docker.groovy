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
        // Container parameters
        string(defaultValue: '192.168.48.2:5002/jenkins/jnlp-image-with-tools:latest', description: '', name: 'TASK_IMAGE', trim: false),
        string(defaultValue: 'pvc-ws', description: '', name: 'TASK_PVC', trim: false),
        string(defaultValue: '8Gi', description: '', name: 'TSK_MEM_LIMIT', trim: false),
        string(defaultValue: '4000m', description: '', name: 'TSK_CPU_LIMIT', trim: false),
        // Genenric parameters
        string(defaultValue: '30', description: '', name: 'TASK_TIMEOUT', trim: false),
        string(defaultValue: 'regcred', description: '', name: 'IMAGE_PULL_SECRETS', trim: false),
        // Task parameters
        string(defaultValue: '192.168.48.2:5002', description: '', name: 'REPOSITORY', trim: true),
        string(defaultValue: 'admin', description: '', name: 'USERNAME', trim: true),
        string(defaultValue: 'SGFyYm9yMTIzNDUK', description: '', name: 'PASSWORD', trim: true),
        string(defaultValue: '.', description: '', name: 'CONTEXT', trim: true),
        string(defaultValue: '192.168.48.2:5002/jenkins/test:v1.0.0', description: '', name: 'TAG', trim: true),
        string(defaultValue: 'Dockerfile', description: '', name: 'DOCKERFILE', trim: true)
        ])
])

// Task parameters
def repo = params.REPOSITORY
def username = params.USERNAME
def password = params.PASSWORD
def tag = params.TAG
def dockerfile = params.DOCKERFILE
def context = params.CONTEXT

// Container parameters
def taskPVC = params.TASK_PVC

def taskTmage = params.TASK_IMAGE
def taskMemoryLimit = params.TSK_MEM_LIMIT
def taskCpuLimit = params.TSK_CPU_LIMIT

// Generic parameters for task
def taskTimeout = params.TASK_TIMEOUT
def imagePullSecrets = params.IMAGE_PULL_SECRETS

// Generic parameters for Pipeline
def mDefaultContainer = "main"
def mCustomWorkspace = '/home/jenkins/agent/workspace/ws'

def mYaml = """\
apiVersion: v1
kind: Pod
metadata:
  name: jenkins-slave
spec:
  containers:
  - name: "jnlp"
    image: "192.168.48.2:5002/jenkins/jnlp-slave:4.7-1-jdk11"
    imagePullPolicy: "Always"
    tty: true
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
  - name: $mDefaultContainer
    image: $taskTmage
    imagePullPolicy: "Always"
    tty: true
    command:
    - cat
    securityContext:
      privileged: true
    volumeMounts:
    - mountPath: "/var/run/docker.sock"
      name: "docker-sock"
      readOnly: false
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: $taskMemoryLimit
        cpu: $taskCpuLimit
  imagePullSecrets:
  - name: $imagePullSecrets
  nodeSelector:
    kubernetes.io/hostname: "node13"
  restartPolicy: Never
  volumes:
  - hostPath:
      path: "/var/run/docker.sock"
    name: "docker-sock"
""".stripIndent()

// Uses Declarative syntax to run commands inside a container.
pipeline {
    agent {
        kubernetes {
            yaml mYaml
            defaultContainer mDefaultContainer
            workspaceVolume persistentVolumeClaimWorkspaceVolume(claimName: taskPVC, readOnly: false)
            customWorkspace mCustomWorkspace
        }
    }

    options { timestamps () }

    stages {
        stage('Task') {
            options {
                timeout(time: taskTimeout, unit: 'MINUTES') 
            }
            steps {
                echo "Task starts =============================================================="
                // Custom Jenkins shared library: dockerImage
                dockerImage repo: repo, username: username, password: password, tag: tag, context: context, dockerfile: dockerfile
                echo "Task ends   =============================================================="
            }
        }
    }
}
