-- ============================================================
-- 테이블명 : users
-- 설명     : 서비스 회원 정보를 관리하는 핵심 테이블
--            일반 회원가입과 소셜 로그인 모두 이 테이블을 단일 주체로 사용한다.
--
-- 설계 원칙
--   - email    : 소셜 가입 시 제공하지 않는 경우가 있으므로 NULL 허용
--   - password : 일반 가입 회원만 사용하며, 소셜 전용 가입 시 NULL
--   - 소셜 로그인 연동 정보는 social_accounts 테이블에서 별도 관리
-- ============================================================

CREATE TABLE users (

    -- 회원 고유 식별자 (자동 증가)
    user_id    BIGINT        NOT NULL IDENTITY(1,1),

    -- 이메일 주소 (소셜 가입 시 미제공 가능 → NULL 허용, 중복 불가)
    email      NVARCHAR(100) NULL,

    -- 비밀번호 해시값 (BCrypt 등 해시 처리 후 저장, 소셜 전용 회원은 NULL)
    password   NVARCHAR(255) NULL,

    -- 서비스 내 표시 이름
    nickname   NVARCHAR(50)  NOT NULL,

    -- 계정 활성 상태 (1: 활성, 0: 비활성/탈퇴)
    is_active  BIT           NOT NULL DEFAULT 1,

    -- 계정 생성 일시
    created_at DATETIME2     NOT NULL DEFAULT GETDATE(),

    -- 계정 정보 최종 수정 일시
    updated_at DATETIME2     NOT NULL DEFAULT GETDATE(),

    -- 기본 키
    CONSTRAINT PK_users PRIMARY KEY (user_id),

    -- 이메일 중복 가입 방지
    CONSTRAINT UQ_users_email UNIQUE (email)
);
