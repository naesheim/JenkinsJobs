pipeline {
	agent any
	stages {
		stage('checkout') {
			steps {
				var token = sh(returnStdout: true, script:'docker run --rm google/cloud-sdk gcloud auth print-access-token').trim()
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
