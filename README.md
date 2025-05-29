## Create Docker Spring Image File

```
docker build -t {app-image-name}:{custom-version} .
```


## Local Docker Testing

1. Run PostgreSQL Container:
```
docker run --name postgres-db \
--network {network_name} \
-e POSTGRES_DB=file-test \
-e POSTGRES_USER=postgres \
-e POSTGRES_PASSWORD=password \
-p 5432:5432 \
-d postgres
```

2. Run Spring Container:
```
docker run --name {app-container-name} \
--network {network_name} \
-e DB_URL=host.docker.internal \
-e DB_PORT=5432 \
-e DB_NAME=file-test \
-e DB_USERNAME=postgres \
-e DB_PASSWORD=password \
-e ALLOWED_ORIGINS=* \
-e PROFILE=dev \
-p 8080:8080 \
-d {app-image-name}
```

## Create Remote Docker Spring Image File
```
docker build -t {docker-username}/{app-image-name}:{custom-version} -t {docker-username}/{app-image-name}:latest .
```
```
docker push {docker-username}/{app-image-name}:{custom-version}
docker push {docker-username}/{app-image-name}:latest
```
