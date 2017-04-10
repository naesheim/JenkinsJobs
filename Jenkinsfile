pipeline {
	agent any
	stages {
		stage('checkout') {
			steps {
				checkout scm
				sh 'token.sh'
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
