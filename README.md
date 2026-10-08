# 📁 Wizlit Path File Service (`file-service`)

> **서비스 포트**: `http://localhost:8081` (Swagger: `http://localhost:8081/documentation`)  
> **기술 스택**: Java 17, Spring Boot 3.4.3, Spring WebFlux, R2DBC (Reactive PostgreSQL), AWS S3 SDK v2  
> **루트 설계도 SSOT**: [`references/runbooks/ARCHITECTURE.md`](../../references/runbooks/ARCHITECTURE.md) | 인프라 세팅: [`SETUP.md`](../../SETUP.md)

---

## 🎯 AI 에이전트 요청 라우팅 매트릭스 (File Service Action Routing)

> 🛑 **필독 지침**: 파일 및 미디어 스트리밍 서비스 작업 시, 아래 매트릭스에 따라 대상 패키지/파일을 즉시 확인하고 논블로킹 I/O 규약(암호)을 준수하십시오.

| 사용자 요청 키워드 & 상황 (Trigger) | 🛑 AI 에이전트 필수 열람 대상 (`view_file`) | 열람하여 확인할 핵심 기준 (Verification) |
| :--- | :--- | :--- |
| **파일 업로드 / Presigned URL 발행 API** | `src/main/java/com/wizlit/file/controller/FileController.java` | S3 Presigned URL 발행 엔드포인트 및 파일 메타데이터 입출력 DTO |
| **AWS S3 연동 및 스트리밍 로직** | `src/main/java/com/wizlit/file/service/S3Service.java` | AWS SDK v2 `S3Presigner` 서명 생성 및 버킷(`AWS_S3_BUCKET`) 연동 |
| **파일 메타데이터 DB 관리 / 서비스** | `src/main/java/com/wizlit/file/service/FileService.java` | 파일 엔티티(`File`) R2DBC 비동기 저장/조회/삭제 트랜잭션 |
| **AWS 클라이언트 설정 및 인증** | `src/main/java/com/wizlit/file/config/AmazonConfig.java` | AWS S3 클라이언트 빈(Bean) 설정 및 리전/인증키 Fallback |
| **파일 전용 DB DDL 마이그레이션** | `src/main/resources/db/migration/V1__Initial_Setup.sql` | `file`, `view` 테이블 스키마 및 외래키/인덱스 명세 |

---

## 🚀 빠른 시작 (Quick Start)

```bash
# 로컬 빌드 및 실행 (Gradle, 포트 8081)
./gradlew bootRun

# 단위 및 통합 테스트 실행
./gradlew test

# Docker 빌드
docker build -t file-service:latest .
```

---

## 🔑 필수 환경변수 (`application.yml` / ENV)

| 변수명 | 필수 여부 | 기본값 (로컬) | 설명 |
| :--- | :---: | :--- | :--- |
| `DB_URL` | **필수** | `localhost` | PostgreSQL 호스트 주소 |
| `DB_PORT` | **필수** | `5432` | PostgreSQL 포트 |
| `DB_NAME` | **필수** | `file-test` | 파일 서비스 전용 데이터베이스 |
| `DB_USERNAME` | **필수** | `postgres` | 데이터베이스 계정명 |
| `DB_PASSWORD` | **필수** | `password` | 데이터베이스 비밀번호 |
| `AWS_S3_BUCKET`| 선택 | `wizlit-path-files` | 미디어/에셋 저장용 S3 버킷명 |
| `ALLOWED_ORIGINS`| 선택 | `*` | CORS 허용 오리진 (프론트: 3000 허용) |
| `PROFILE` | 선택 | `dev` | 스프링 활성 프로파일 (`dev`, `prod`) |

---

## 📂 핵심 패키지 구조 및 분장

| 패키지 경로 | 역할 및 핵심 클래스 |
| :--- | :--- |
| `src/main/java/com/wizlit/file/controller/` | 파일 Presigned URL 발급 및 메타데이터 REST 컨트롤러 (`FileController`) |
| `src/main/java/com/wizlit/file/service/` | S3 연동 및 파일 비즈니스 로직 (`FileService`, `S3Service`, `FileManager`) |
| `src/main/java/com/wizlit/file/config/` | S3 연동 빈, R2DBC 리액티브 설정, CORS (`AmazonConfig`, `R2dbcConfig`) |
| `src/main/java/com/wizlit/file/entity/` | 파일 및 뷰 테이블 R2DBC 엔티티 (`File`, `View`) |
| `src/main/java/com/wizlit/file/repository/` | R2DBC 논블로킹 리포지토리 (`FileRepository`, `ViewRepository`) |
| `src/main/resources/db/migration/` | Flyway 데이터베이스 형상 관리 (`V1__Initial_Setup.sql`) |

---

## 💡 개발 시 알아두면 유용한 사실 (File Service Gotchas)

1. **미디어 I/O 서버 부하 격리 원칙 (Architecture Rule)**:
   - 본 마이크로서비스는 대용량 동영상/이미지 트래픽이 메인 백엔드(`8080`)의 리액티브 이벤트 루프를 고갈시키지 않도록 **물리적으로 완전히 분리된 전용 포트(`8081`) 서비스**입니다.
2. **S3 Presigned URL 서명 방식**:
   - 파일 본문 바이너리를 서버 메모리로 버퍼링하지 않고, 클라이언트가 AWS S3로 직접 업로드/다운로드할 수 있도록 짧은 만료 시간의 Presigned URL을 즉시 반환합니다.
3. **독립 데이터베이스 (`file-test`)**:
   - 메인 백엔드의 `test` 데이터베이스와 별도로 PostgreSQL 내의 `file-test` 데이터베이스를 사용하므로, 로컬 DB 초기화 시 `file-test` DB의 존재 여부를 반드시 확인해야 합니다.