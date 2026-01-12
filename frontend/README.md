# Frontend Seed (React + JavaScript)

- 기존 SpringBoot `src/main/resources/static/assets` 를 `public/assets` 로 복사해두고,
  기존 CSS/이미지 경로(`/assets/...`)를 유지한 채 React로 페이지를 재작성하는 초기 골격입니다.

## Run
```bash
npm install
cp .env.example .env
npm run dev
```

## Next
- `src/pages/*` 파일들에 기존 Thymeleaf 템플릿 내용을 JSX로 옮기세요.
- 백엔드가 HTML 렌더링 대신 `/api/**` JSON을 제공하도록 바꾸면,
  React에서 `src/api/http.js`로 호출해 화면을 완성할 수 있습니다.
