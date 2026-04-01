-- ============================================================
-- 테이블명 : social_accounts
-- 설명     : 소셜 로그인 연동 정보를 관리하는 테이블
--            users 테이블과 1:N 관계로, 하나의 계정에
--            카카오·네이버·구글을 모두 연동할 수 있다.
--
-- 설계 원칙
--   - user_id     : users 테이블의 회원 식별자 (FK 미설정, 애플리케이션 레벨에서 관리)
--   - provider    : 소셜 플랫폼 구분값 ('KAKAO' | 'NAVER' | 'GOOGLE')
--   - provider_id : 각 소셜 플랫폼이 발급한 회원 고유 ID
--   - (provider, provider_id) 조합으로 소셜 계정 중복 가입 방지
-- ============================================================

CREATE TABLE social_accounts (

    -- 소셜 연동 고유 식별자 (자동 증가)
    social_account_id BIGINT        NOT NULL IDENTITY(1,1),

    -- 연동된 서비스 회원 ID (users.user_id 참조, FK 미설정)
    user_id           BIGINT        NOT NULL,

    -- 소셜 플랫폼 구분 ('KAKAO' | 'NAVER' | 'GOOGLE')
    provider          NVARCHAR(20)  NOT NULL,

    -- 소셜 플랫폼에서 발급한 회원 고유 ID
    provider_id       NVARCHAR(100) NOT NULL,

    -- 소셜 플랫폼에서 제공한 이메일 (미제공 시 NULL)
    provider_email    NVARCHAR(100) NULL,

    -- 소셜 연동 등록 일시
    created_at        DATETIME2     NOT NULL DEFAULT GETDATE(),

    -- 기본 키
    CONSTRAINT PK_social_accounts PRIMARY KEY (social_account_id),

    -- 동일 소셜 계정의 중복 연동 방지 (플랫폼 + 플랫폼 고유ID 조합)
    CONSTRAINT UQ_social_provider UNIQUE (provider, provider_id)
);
