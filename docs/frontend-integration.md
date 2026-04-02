# 프론트엔드 연동 가이드

> 백엔드: Spring Boot 3.4.4 · 포트 `8080`  
> 프론트엔드: React · 포트 `8088`

---

## 목차

1. [기본 설정](#1-기본-설정)
2. [소셜 로그인](#2-소셜-로그인)
3. [인증 흐름](#3-인증-흐름)
4. [API 요청 방법](#4-api-요청-방법)
5. [로그아웃](#5-로그아웃)
6. [공개 API 목록](#6-공개-api-목록)
7. [에러 처리](#7-에러-처리)

---

## 1. 기본 설정

### axios 전역 설정

모든 API 요청에 쿠키를 자동으로 포함하려면 `withCredentials: true`를 반드시 설정해야 합니다.

```js
// src/api/axios.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true, // httpOnly 쿠키 자동 전송에 필수
});

export default api;
```

### fetch 사용 시

```js
fetch('http://localhost:8080/api/...', {
  credentials: 'include', // httpOnly 쿠키 자동 전송에 필수
});
```

---

## 2. 소셜 로그인

### 로그인 버튼 구현

브라우저를 백엔드 OAuth2 엔드포인트로 직접 이동시킵니다.  
백엔드가 소셜 플랫폼과의 인가 코드 교환을 모두 처리합니다.

```js
// 카카오 로그인
const loginWithKakao = () => {
  window.location.href = 'http://localhost:8080/api/oauth2/authorization/kakao';
};

// 네이버 로그인
const loginWithNaver = () => {
  window.location.href = 'http://localhost:8080/api/oauth2/authorization/naver';
};
```

### 로그인 완료 후 처리

로그인 성공 시 백엔드가 `http://localhost:8088/` 로 리다이렉트합니다.  
JWT는 **httpOnly 쿠키**(`access_token`)로 브라우저에 자동 저장되므로, 프론트엔드에서 토큰을 직접 다룰 필요가 없습니다.

```
로그인 완료 리다이렉트 URL: http://localhost:8088/
저장 방식: Set-Cookie: access_token=<JWT>; HttpOnly; Path=/; SameSite=Lax
```

> **주의:** 토큰을 localStorage나 JS 변수에 저장하지 마세요. httpOnly 쿠키는 JS로 접근할 수 없으며, 이것이 XSS 방어의 핵심입니다.

---

## 3. 인증 흐름

```
1. 프론트  →  window.location.href = /api/oauth2/authorization/kakao
2. 백엔드  →  카카오 로그인 페이지로 리다이렉트
3. 사용자     카카오에서 로그인
4. 카카오  →  백엔드 /api/login/oauth2/code/kakao 로 인가코드 전달
5. 백엔드     사용자 정보 조회 → DB upsert → JWT 생성
6. 백엔드  →  Set-Cookie: access_token=JWT; HttpOnly
             Location: http://localhost:8088/
7. 프론트     이후 모든 요청에 브라우저가 쿠키 자동 포함
```

---

## 4. API 요청 방법

### 인증이 필요한 API 호출

추가 헤더 없이 `withCredentials: true`만 설정하면 됩니다.  
브라우저가 httpOnly 쿠키를 자동으로 요청에 포함시킵니다.

```js
// axios 사용 예시
import api from './api/axios';

const getUserInfo = async () => {
  const response = await api.get('/api/users/me');
  return response.data;
};
```

```js
// fetch 사용 예시
const getUserInfo = async () => {
  const response = await fetch('http://localhost:8080/api/users/me', {
    credentials: 'include',
  });
  if (!response.ok) throw new Error('인증 필요');
  return response.json();
};
```

### 로그인 상태 확인

인증이 필요한 API에 요청했을 때 `401` 응답이 오면 미로그인 상태입니다.

```js
// axios interceptor로 전역 인증 만료 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 로그인 페이지로 이동 또는 소셜 로그인 유도
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 5. 로그아웃

`POST /api/auth/logout` 을 호출하면 백엔드가 `access_token` 쿠키를 만료시킵니다.

```js
const logout = async () => {
  await api.post('/api/auth/logout');
  // 쿠키가 삭제됨 → 로그인 페이지로 이동
  window.location.href = '/login';
};
```

| 항목 | 값 |
|------|-----|
| 메서드 | `POST` |
| URL | `http://localhost:8080/api/auth/logout` |
| 인증 | 불필요 (공개 엔드포인트) |
| 성공 응답 | `200 OK` |

---

## 6. 공개 API 목록

인증 없이 호출 가능한 엔드포인트입니다.

| 메서드 | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/health` | 서버 상태 확인 |
| `GET` | `/api/oauth2/authorization/kakao` | 카카오 로그인 시작 |
| `GET` | `/api/oauth2/authorization/naver` | 네이버 로그인 시작 |
| `POST` | `/api/auth/logout` | 로그아웃 |
| `GET` | `/swagger-ui/index.html` | API 문서 (Swagger UI) |

---

## 7. 에러 처리

| HTTP 상태 | 의미 | 프론트 처리 |
|-----------|------|-------------|
| `200` | 성공 | 정상 처리 |
| `401 Unauthorized` | 미로그인 또는 토큰 만료 | 로그인 페이지로 이동 |
| `403 Forbidden` | 권한 없음 | 접근 불가 안내 |
| `500 Internal Server Error` | 서버 오류 | 에러 메시지 표시 |

---

## 소셜 플랫폼 개발자 콘솔 설정 (백엔드 담당자 확인용)

로컬 개발 환경에서 소셜 로그인이 동작하려면 각 플랫폼 콘솔에 Redirect URI를 등록해야 합니다.

| 플랫폼 | 등록할 Redirect URI |
|--------|---------------------|
| 카카오 | `http://localhost:8080/api/login/oauth2/code/kakao` |
| 네이버 | `http://localhost:8080/api/login/oauth2/code/naver` |
