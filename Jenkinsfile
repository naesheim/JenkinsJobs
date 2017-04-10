pipeline {
	agent any

	environment {
		token = sh(returnStdout: true, script:'docker run --rm google/cloud-sdk gcloud auth print-access-token').trim()
	}

	stages {
		stage('checkout') {
			steps {
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
