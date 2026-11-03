# 갈곳 화면(React) 이미지: npm 빌드 결과를 Caddy가 직접 내보낸다
# 1단계: 빌드
FROM node:22-alpine AS build
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# 화면과 API가 같은 주소라 API 주소는 배포 주소 그대로(docker-compose에서 넘김)
ARG VITE_API_BASE
# 카카오 지도 JavaScript 키. 브라우저에 보이는 키라 빌드 때 들어간다(카카오 콘솔 도메인 제한으로 보호)
ARG VITE_KAKAO_MAP_KEY
ENV VITE_API_BASE=$VITE_API_BASE \
    VITE_KAKAO_MAP_KEY=$VITE_KAKAO_MAP_KEY
RUN npm run build

# 2단계: Caddy(HTTPS 인증서 자동 발급, 정적 파일, /api 넘기기). 설정은 docker-compose에서 deploy/Caddyfile을 붙인다
FROM caddy:2-alpine
COPY --from=build /web/dist /srv
