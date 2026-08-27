# syntax=docker/dockerfile:1
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# Runtime enxuto: reinstala só as dependências de produção (sem devDependencies/toolchain de
# build) e copia apenas o necessário para rodar `next start` — evita levar node_modules completo
# (incl. eslint/vitest/typescript) da stage de build para a imagem final (critério de aceite,
# TASK-08.3).
FROM node:22-alpine
RUN addgroup -S kanban && adduser -S kanban -G kanban
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev && npm cache clean --force
COPY --from=build /app/.next ./.next
# .next/cache é cache de build (webpack/SWC) do `next build` na stage anterior — nunca usado em
# runtime pelo `next start`, só infla a imagem final (achado de tamanho, TASK-08.3).
RUN rm -rf ./.next/cache
COPY --from=build /app/next.config.mjs ./next.config.mjs
USER kanban
EXPOSE 3000
CMD ["npm", "start"]
