pipeline {
	agent any
	stages {
		stage('checkout') {
			steps {
				sh token.sh
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
