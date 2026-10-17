# 📁 `file-service`

> 🎯 **목적**: 대용량 미디어 I/O 부하 격리, S3 Presigned URL 발급 및 비동기 파일 메타데이터 관리 마이크로서비스  
> 🧭 **단일 원천(SSOT)**: [`/AGENTS.md`](/AGENTS.md)  
> 📑 **참조 문서**: [`/references/runbooks/ARCHITECTURE.md`](/references/runbooks/ARCHITECTURE.md), [`/SETUP.md`](/SETUP.md)

---

## 1. 프로젝트 개요 및 아키텍처

- **서비스 포트**: `http://localhost:8081` (Swagger: `http://localhost:8081/documentation`)
- **기술 스택**: Java 17, Spring Boot 3.4.3, Spring WebFlux, R2DBC (Reactive PostgreSQL), AWS S3 SDK v2, Maven
- **미디어 I/O 부하 격리 원칙**:
  - 대용량 동영상/이미지 업로드 트래픽이 메인 백엔드(`8080`)의 Netty 이벤트 루프를 고갈시키지 않도록 물리적으로 완전히 격리된 독립 포트(`8081`)로 운용.
  - 서버 메모리 버퍼링을 지양하고 S3 Presigned URL 서명 발급을 통해 클라이언트-S3 간 직접 업로드/다운로드 수행.

---

## 2. 파일 구성 및 패키지 분장

| 패키지 / 디렉토리 경로 | 역할 및 핵심 클래스 |
| :--- | :--- |
| `src/main/java/com/wizlit/file/controller/` | 파일 Presigned URL 발급 및 메타데이터 REST 컨트롤러 (`FileController`) |
| `src/main/java/com/wizlit/file/service/` | S3 연동 및 파일 비즈니스 로직 (`FileService`, `S3Service`, `FileManager`) |
| `src/main/java/com/wizlit/file/config/` | S3 연동 빈, R2DBC 리액티브 설정, CORS (`AmazonConfig`, `R2dbcConfig`) |
| `src/main/java/com/wizlit/file/entity/` | 파일, 뷰 및 유저 저장소 쿼터 R2DBC 엔티티 (`File`, `View`, `UserStorageQuota`) |
| `src/main/java/com/wizlit/file/repository/` | R2DBC 논블로킹 리포지토리 (`FileRepository`, `ViewRepository`, `UserStorageQuotaRepository`) |
| `src/main/resources/db/migration/` | Flyway 데이터베이스 형상 관리 (`V1__Initial_Setup.sql`, `V2__Transfer_Admin_Files.sql`, `V3__Add_Memo_Id_And_Status.sql`, `V4__Create_User_Storage_Quota.sql`) |

---

## 3. 관문 라우팅 표

| 🎯 발동 트리거 (상황/키워드) | 🛑 필수 열람 대상 | 핵심 기준 (확인 암호) |
| :--- | :--- | :--- |
| 파일 업로드 및 Presigned URL 발급 | `FileController.java` | S3 Presigned URL 발행 엔드포인트 및 DTO 입출력 확인 |
| AWS S3 SDK 연동 및 버킷 제어 | `S3Service.java`<br>`AmazonConfig.java` | `S3Presigner` 서명 생성 및 환경변수(`AWS_S3_BUCKET`) 일치 |
| 파일 메타데이터 및 쿼터 관리 | `FileService.java`<br>`FileRepository.java`<br>`UserStorageQuotaRepository.java` | R2DBC 리액티브 트랜잭션, 월간 쿼터 검증 및 논블로킹 반환 준수 |
| 파일 서비스 전용 DB 스키마 형상 | `db/migration/` (`V1`~`V4`) | `file`, `view`, `user_storage_quota` 테이블 스키마 및 외래키/인덱스 명세 |

---

## 4. 환경 변수 및 실행 가이드

### 4.1 필수 환경 변수 (`.env` / `application.yml`)

| 변수명 | 필수 여부 | 기본값 (로컬) | 설명 |
| :--- | :---: | :--- | :--- |
| `PORT` | 선택 | `8081` | 파일 서비스 구동 포트 |
| `PROFILE` | 선택 | `dev` | 스프링 활성 프로파일 (`dev`, `prod`) |
| `DB_URL` | **필수** | `localhost` | PostgreSQL 호스트 주소 |
| `DB_PORT` | **필수** | `5432` | PostgreSQL 포트 |
| `DB_NAME` | **필수** | `file_test` | 파일 서비스 전용 데이터베이스 |
| `DB_USERNAME` | **필수** | `postgres` | 데이터베이스 계정명 |
| `DB_PASSWORD` | **필수** | `password` | 데이터베이스 비밀번호 |
| `AWS_S3_URL` | 선택 | `https://s3.us-east-1.amazonaws.com` | S3 엔드포인트 URL |
| `AWS_S3_REGION` | 선택 | `us-east-1` | AWS S3 리전 |
| `AWS_S3_BUCKET` | 선택 | `wizlit-test-bucket` | 미디어/에셋 저장용 S3 버킷명 |
| `AWS_S3_ACCESS_KEY` | 선택 | `dummy_test_key` | AWS 액세스 키 (로컬 Mock/운영) |
| `AWS_S3_SECRET_KEY` | 선택 | `dummy_test_secret` | AWS 시크릿 키 (로컬 Mock/운영) |

### 4.2 실행 명령어 (Maven)

```bash
# 로컬 빌드 및 실행 (Maven, 포트 8081)
.\mvnw.cmd spring-boot:run

# 단위 및 통합 테스트 실행
.\mvnw.cmd test

# Docker 빌드
docker build -t file-service:latest .
```

---

## 5. 핵심 규칙 및 Gotchas

1. **독립 데이터베이스 분리 (`file_test`)**:
   - 메인 백엔드의 `test` 데이터베이스와 별도로 PostgreSQL 내의 `file_test` 데이터베이스를 사용하므로, 로컬 환경 셋업 시 `file_test` DB 존재 여부를 필수 점검함.
2. **S3 Presigned URL 반환 규약**:
   - 대용량 파일 본문을 WAS 메모리로 수신하지 않고 만료 시간을 가진 서명 URL을 클라이언트에 전달하여 S3와 직접 통신하도록 제어함.
3. **루트 기준 절대 링크 원칙 (`02-1`)**:
   - 문서 내 모든 상호 참조는 `scripts/check-links.py`가 해석 가능한 루트 기준 절대 경로(`/...`)를 엄수함.