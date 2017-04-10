pipeline {
	agent {
		docker 'python:slim'
	}
	environment {
		key = "${docker run --rm -ti google/cloud-sdk gcloud auth print-access-token}"
	}
	stages {
		stage('runScript') {	
			steps {
				sh 'python scripts/hello.py'
				echo "${key}"
			}
		}
	}
}
