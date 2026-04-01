# Jiin Backend

## 프로젝트 개요

Jiin 서비스의 백엔드 서버입니다.

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.4.4 |
| Gradle | 8.11.1 |
| 빌드 도구 | Gradle Wrapper |

---

## 의존성

| 라이브러리 | 용도 |
|-----------|------|
| spring-boot-starter-web | REST API 서버 |
| spring-boot-starter-actuator | 헬스체크 및 모니터링 |
| lombok | 보일러플레이트 코드 제거 |
| spring-boot-starter-test | 테스트 |

---

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/jiin/backend/
│   │   ├── JiinBackendApplication.java   # 애플리케이션 진입점
│   │   └── controller/
│   │       └── HealthController.java     # 헬스체크 API
│   └── resources/
│       └── application.properties        # 서버 설정
└── test/
    └── java/com/jiin/backend/
        └── JiinBackendApplicationTests.java
```

---

## 실행 방법

```bash
# 서버 실행
./gradlew bootRun

# 빌드
./gradlew build

# 테스트
./gradlew test
```

> Windows 환경에서는 `./gradlew` 대신 `gradlew.bat`을 사용합니다.

---

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/health` | 서버 상태 확인 |
| GET | `/actuator/health` | Spring Actuator 헬스체크 |

---

## 서버 설정

- 기본 포트: `8080`
- Actuator 노출 엔드포인트: `health`, `info`
