docker login -u oauth2accesstoken -p $(docker run --rm -i google/cloud-sdk gcloud auth print-access-token) https://gcr.io
