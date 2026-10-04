# 📁 Wizlit Path File Service (`file-service`)

> **서비스 포트**: `http://localhost:8081`  
> **기술 스택**: Java 17, Spring Boot 3.4.3, Spring WebFlux, R2DBC, AWS S3 SDK v2  
> **루트 설계도 SSOT**: [`references/runbooks/ARCHITECTURE.md`](../../references/runbooks/ARCHITECTURE.md) | 세팅: [`SETUP.md`](../../SETUP.md)

---

## 🚀 빠른 시작 (Quick Start)

```bash
# 로컬 빌드 및 실행 (Gradle)
./gradlew bootRun

# Docker 빌드
docker build -t file-service:latest .
```

---

## 🔑 필수 환경변수 (`application.yml` / ENV)

| 변수명 | 필수 여부 | 기본값 (로컬) | 설명 |
| :--- | :---: | :--- | :--- |
| `DB_URL` | **필수** | `localhost` (또는 `host.docker.internal`) | PostgreSQL 호스트 주소 |
| `DB_PORT` | **필수** | `5432` | PostgreSQL 포트 |
| `DB_NAME` | **필수** | `file-test` | 파일 서비스 전용 DB |
| `DB_USERNAME` | **필수** | `postgres` | 데이터베이스 계정명 |
| `DB_PASSWORD` | **필수** | `password` | 데이터베이스 비밀번호 |
| `AWS_S3_BUCKET`| 선택 | `wizlit-path-files` | 미디어/에셋 저장용 S3 버킷명 |
| `ALLOWED_ORIGINS`| 선택 | `*` | CORS 허용 오리진 |
| `PROFILE` | 선택 | `dev` | 스프링 활성 프로파일 (`dev`, `prod`) |