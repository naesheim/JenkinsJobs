pipeline {
	agent any
	stages {
		stage('checkout') {
			steps {
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
