# E2E 수동 테스트 가이드

단위 테스트(Mock)로 검증할 수 없는 실제 서비스 동작을 확인하는 가이드.  
실제 DB, AI 서버, LLM 응답을 모두 거치는 통합 테스트입니다.

---

## 사전 준비

1. **Docker 컨테이너 기동** (PostgreSQL + AI 서버)
   ```bash
   docker compose up -d
   ```

2. **Spring Boot 실행** (IntelliJ Run)
   - `.env` 파일이 Run Configuration에 연결되어 있어야 함

3. **Swagger UI 접속**
   ```
   http://localhost:8080/api/swagger-ui/index.html
   ```

---

## 테스트 흐름

### Step 1. 회원가입
`POST /auth/register`
```json
{
  "email": "test@test.com",
  "password": "password123",
  "name": "테스터"
}
```
✅ 확인: `201 Created`, 에러 없음

---

### Step 2. 로그인 + 토큰 설정
`POST /auth/login`
```json
{
  "email": "test@test.com",
  "password": "password123"
}
```
✅ 확인: `accessToken` 반환  
→ Swagger 우상단 **Authorize** 버튼 클릭 → `Bearer {accessToken}` 입력

---

### Step 3. 주제 등록
`POST /topics`
```json
{
  "title": "머신러닝 기초"
}
```
✅ 확인: `topicId` 반환, DB에 저장됨

---

### Step 4. 목차 생성 (LLM 호출)
`POST /topics/{topicId}/outline`  
(body 없음)

✅ 확인: 챕터 4~6개 + 키워드가 포함된 JSON 반환  
⚠️ 실패 시: AI 서버 로그 확인 (`docker logs skai-ai-server`)

---

### Step 5. 학습 세션 시작
`POST /sessions`
```json
{
  "topicId": 1
}
```
✅ 확인: `sessionId` 반환, status = `ACTIVE`

---

### Step 6. AI 튜터 채팅 (LLM 호출)
`POST /sessions/{sessionId}/chat`
```json
{
  "message": "머신러닝이 뭔가요?"
}
```
✅ 확인: AI 튜터 응답 텍스트 반환  
✅ 확인: 소크라테스식 대화 (질문/설명 형식)

2~3턴 더 대화해보기:
```json
{ "message": "지도학습과 비지도학습의 차이가 뭔가요?" }
{ "message": "과적합은 왜 발생하나요?" }
```

---

### Step 7. 세션 종료 + 역설명 질문 생성 (LLM 호출)
`POST /sessions/{sessionId}/finish`  
(body 없음)

✅ 확인: 질문 3~5개 반환 (model_answer **미포함**)  
✅ 확인: DB에 `evaluation` 레코드 생성됨  
⚠️ model_answer가 응답에 포함되면 안 됨 (보안 규칙)

---

### Step 8. 역설명 채점 (AI 서버 + LLM 호출)
`POST /sessions/{sessionId}/evaluate`

Step 7에서 받은 questionId를 사용:
```json
{
  "answers": [
    { "questionId": 1, "userAnswer": "머신러닝은 데이터로 패턴을 학습하는 AI 기술입니다." },
    { "questionId": 2, "userAnswer": "지도학습은 레이블이 있는 데이터를 사용합니다." }
  ]
}
```
✅ 확인: 각 질문별 점수 + 전체 평균 점수 반환  
✅ 확인: `feedback` 텍스트 반환  
✅ 확인: 이번엔 `modelAnswer` **포함** (채점 완료 후에만 공개)  
✅ 확인: `nextReviewDate` = 내일 날짜

---

### Step 9. 오늘 복습 카드 조회
`GET /reviews/today`

✅ 확인: Step 8에서 생성된 카드 목록 반환  
⚠️ `modelAnswer` **미포함** 확인

---

### Step 10. 복습 채점 (AI 서버 호출)
`POST /reviews/{questionId}/complete`
```json
{
  "answer": "머신러닝은 데이터로부터 패턴을 학습하는 알고리즘입니다."
}
```
✅ 확인: score, modelAnswer, nextReviewDate, reviewCount 반환  
✅ 확인: SM-2 적용 — 점수 80점 이상이면 `nextReviewDate`가 6일 후

---

### Step 11. 주제 히스토리 조회
`GET /topics/{topicId}/history`

✅ 확인: 세션 목록 + 평가 결과 + 복습 현황 통합 조회

---

### Step 12. 알림 확인
`GET /notifications`

✅ 확인: (복습 스케줄러는 09:00에 동작하므로 당일 확인은 어려울 수 있음)

---

## 체크리스트 요약

| 항목 | 확인 |
|------|------|
| LLM 응답이 실제로 오는가 | |
| outline JSON 파싱이 정상인가 | |
| 채팅이 대화 맥락을 유지하는가 | |
| model_answer가 finish/today 응답에서 숨겨지는가 | |
| 채점 점수가 0~100 범위인가 | |
| SM-2 nextReviewDate가 점수에 따라 다른가 | |
| 복습 후 reviewCount가 증가하는가 | |

---

## 문제 발생 시

```bash
# AI 서버 로그
docker logs skai-ai-server -f

# PostgreSQL 접속
docker exec -it skAI-castle-postgres psql -U skAI-castle -d skAI-castle

# Spring 로그는 IntelliJ 콘솔에서 확인
```
