pipeline {
	agent any
	stages {
		stage('checkout') {
			steps {
				script{
					def token = "${docker run --rm google/cloud-sdk gcloud auth print-access-token}"
				}
				echo "${token}"
				checkout scm
			}
		}

		stage('runScript') {
			agent {
				docker 'python:slim'
			}
			steps {
				sh 'python scripts/hello.py'
			}
		}
	}
}
