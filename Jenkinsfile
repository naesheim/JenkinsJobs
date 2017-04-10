pipeline {
	agent {
		docker 'python:slim'
	}
	stages {
		stage('runScript') {	
			steps {
				var token = sh(returnStdout: true, script:'docker run --rm -ti google/cloud-sdk gcloud auth print-access-token' ).trim()
				sh 'python scripts/hello.py'
				echo "${token}"
			}
		}
	}
}
