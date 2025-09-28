FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml* yarn.lock* package-lock.json* ./
RUN npm i -g pnpm && \
    if [ -f pnpm-lock.yaml ]; then pnpm i --frozen-lockfile; \
    elif [ -f yarn.lock ]; then yarn --frozen-lockfile; \
    else npm ci; fi

FROM node:20-alpine AS build
WORKDIR /app
COPY . .
# 复用依赖，保证有 next 可执行
COPY --from=deps /app/node_modules ./node_modules
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm i -g pnpm && \
    if [ -f pnpm-lock.yaml ]; then pnpm run build; \
    elif [ -f yarn.lock ]; then yarn build; \
    else npm run build; fi

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production \
    PORT=3000 \
    NEXT_TELEMETRY_DISABLED=1 \
    BACKEND_BASE_URL=http://backend:8080
# ⬇️ 三个必须复制：standalone、static、public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node","server.js"]
