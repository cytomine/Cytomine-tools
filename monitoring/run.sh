docker stop monitoring
docker rm monitoring
docker build -t="cytomine/monitoring" .
docker run -d --name monitoring -p 8080:8000 cytomine/monitoring
