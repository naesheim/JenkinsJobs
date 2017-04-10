pipeline {
	agent {
		docker 'python:slim'
	}
	environment {
		key = "${gcloud auth print-access-token}"
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
