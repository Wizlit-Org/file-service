# 파일 스토리지 서비스 관문

> 🎯 **목적**: 오브젝트 스토리지(S3 / MinIO) 연동, 파일 업로드/다운로드 스트리밍 및 파일 메타데이터 비즈니스 로직 제공.  
> 🧭 **단일 원천(SSOT)**: [`/AGENTS.md`](/AGENTS.md)

---

## 1. 개요 및 구조
독립 마이크로서비스인 `file-service`의 핵심 비즈니스 로직 계층. 범용 파일 메타데이터 관리 인터페이스(`FileService`), AWS S3 / MinIO 스토리지 I/O 처리 인터페이스(`S3Service`), 구현체(`impl/`), 스토리지 할당량 및 권한 매니저(`manager/`)로 분리됨.

## 2. 파일별 책임 라우팅 표

| 대상 경로 / 파일 | 유형 | 핵심 책임 및 역할 |
| :--- | :---: | :--- |
| `FileService.java` | 인터페이스 | 파일 업로드 메타데이터 등록, 다운로드 링크 발급, 파일 삭제 및 조회 비즈니스 명세 |
| `S3Service.java` | 인터페이스 | S3 버킷 파일 Put/Get 객체 스트리밍, Presigned URL 생성, 멀티파트 업로드 저수준 I/O 명세 |
| `impl/` | 디렉토리 | 서비스 인터페이스별 구체 구현 클래스 (`*ServiceImpl.java`) 수납 |
| `manager/` | 디렉토리 | 사용자별 스토리지 쿼터(Quota) 체크, 파일 MIME 타입 검증, 썸네일 생성 매니저 수납 |

## 3. 핵심 가드레일 (Gotchas)
- **메모리 버퍼링 차단**: 대용량 파일 업로드/다운로드 시 `byte[]` 전체를 힙 메모리에 적재하지 않고 `InputStream` / `OutputStream` 파이프라인으로 스트리밍 처리.
- **S3 연결 풀 및 리소스 해제**: S3 클라이언트의 HTTP 커넥션 누수 방지를 위해 Try-with-resources 또는 확실한 스트림 `close()` 보장.
- **용량 쿼터 선검증**: 실제 스토리지 업로드 전 `UserStorageQuota`를 선행 조회하여 초과 시 즉시 413(Payload Too Large) 차단.
