pipeline {
	agent {
		docker {
			label 'master'
			image 'python:slim'
			args '-it -v $PWD/scripts:/scripts'
		}
	}
	stages {
		stage('runScript') {	
			steps {
				checkout scm
				sh 'python scripts/hello.py'
			}
		}
	}
}
