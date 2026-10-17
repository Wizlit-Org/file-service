# 파일 메타데이터 레포지토리 관문

> 🎯 **목적**: 업로드 파일 메타데이터, 사용자 스토리지 할당량(Quota) 및 파일 조회 이력의 영속성 계층 인터페이스 제공.  
> 🧭 **단일 원천(SSOT)**: [`/AGENTS.md`](/AGENTS.md)

---

## 1. 개요 및 구조
`file-service`의 데이터베이스 접근 계층. 물리적 스토리지 객체와 1:1로 매핑되는 파일 메타데이터(`FileRepository`), 사용자별 사용 용량 제한 통제(`UserStorageQuotaRepository`), 파일 접근/다운로드 감사 통계(`ViewRepository`)를 영속화함.

## 2. 파일별 책임 라우팅 표

| 대상 경로 / 파일 | 유형 | 핵심 책임 및 역할 |
| :--- | :---: | :--- |
| `FileRepository.java` | 인터페이스 | 파일 고유 식별자(UUID/ID), 원본 파일명, S3 Key, 파일 크기, 해시값 CRUD 및 조회 |
| `UserStorageQuotaRepository.java` | 인터페이스 | 사용자별 스토리지 최대 허용량, 현재 사용량 조회 및 용량 증감 원자적 갱신 |
| `ViewRepository.java` | 인터페이스 | 파일 뷰어/다운로드 접근 로그 및 조회수 카운트 영속화 |

## 3. 핵심 가드레일 (Gotchas)
- **스토리지 쿼터 동시성 제어**: 동시 업로드 발생 시 `UserStorageQuota` 용량 갱신 충돌을 방지하기 위해 비관적 락(Pessimistic Lock) 또는 원자적 DB 갱신 쿼리(`UPDATE ... SET used = used + :size`) 활용.
- **물리 파일과 메타데이터 무결성**: DB 메타데이터 삭제 시 S3 실제 파일 삭제가 비동기 큐나 보상 트랜잭션으로 연계되어 고아 파일(Orphan Object) 방지.
- **UUID 기반 접근**: 외부 노출 파일 링크는 순차 증가 ID 대신 예측 불가능한 UUID를 키로 조회하여 무단 파일 열람 방어.
