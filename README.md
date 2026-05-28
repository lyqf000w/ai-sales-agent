# AI Sales Agent

AI Sales Agent is a private-domain sales assistant system for WeChat-based customer operations. It combines CRM-style customer management, AI-assisted replies, human takeover workflows, and GeWe-based WeChat message integration into one console for sales and customer service teams.

This repository is a portfolio-friendly snapshot of the project. Deployment packages, logs, local environment files, and private credentials are intentionally excluded.

## Features

- WeChat private chat and group chat conversation workbench
- Customer profile, tags, sales stage, purchase intention, sentiment, and follow-up priority management
- AI auto-reply workflow with manual confirmation and human takeover queues
- Configurable human escalation rules for high-risk or sensitive conversations
- Built-in fallback for non-text messages such as images, emoji, voice, files, and videos
- GeWe callback parsing for inbound WeChat messages
- Media download and display support for images, emoji, voice, files, and videos
- WeChat CDN media proxy with HTTPS compatibility and encrypted media handling
- Multi-tenant and multi-WeChat-account data isolation
- Operational console for account binding, conversation handling, tags, rules, knowledge base, and statistics

## Tech Stack

- Backend: Java 17, Spring Boot 3, MyBatis Plus
- Frontend: Vue 3, TypeScript, Element Plus, Vite
- Database: PostgreSQL
- Cache and infrastructure: Redis, Nginx
- AI and messaging: DeepSeek-compatible LLM integration, GeWe API
- Build tools: Maven, pnpm

## Project Structure

```text
.
+-- sales-server                  # Spring Boot application entry
+-- sales-module-agent            # AI sales agent domain module
+-- sales-module-system           # system, auth, tenant, and user modules
+-- sales-module-infra            # infrastructure modules
+-- sales-framework               # shared framework starters and utilities
+-- sales-ui/sales-ui-admin       # Vue 3 admin console
+-- sql/postgresql                # PostgreSQL migration scripts
+-- docs                          # design notes and deployment notes
`-- script                        # helper scripts and docker examples
```

## Core Module Highlights

### Conversation Workbench

The console separates private chats and group chats, keeps message lists scoped by tenant and WeChat account, and provides a focused customer service workflow for sales teams.

### AI and Human Collaboration

The reply pipeline supports automatic replies, manual confirmation, human takeover, and escalation alerts. Media messages and high-risk intents can be routed to human handling to avoid inappropriate AI responses.

### GeWe Integration

The backend integrates with GeWe callbacks and media download APIs. It handles text, image, emoji, voice, video, and file messages, then normalizes them for display in the console.

### Media Compatibility

The project includes a WeChat media proxy for resources that cannot be loaded directly by browsers because of mixed content, certificate problems, or encrypted CDN payloads. Browser-incompatible voice formats such as Silk are handled with clear fallback behavior.

### Data Isolation

Conversation, contact, and message records are isolated by tenant and WeChat account. This avoids data mixing when one tenant connects multiple WeChat accounts.

## Local Development

Backend:

```bash
mvn -pl sales-server -am spring-boot:run
```

Frontend console:

```bash
cd sales-ui/sales-ui-admin
pnpm install
pnpm dev
```

Build frontend console:

```bash
cd sales-ui/sales-ui-admin
pnpm build:console
```

Run backend tests for the agent module:

```bash
mvn -pl sales-module-agent -am test
```

## Configuration

Real environment files and credentials are not included in this repository. Configure these values in local or server-side environment files:

- PostgreSQL connection
- Redis connection
- GeWe API base URL and token
- GeWe callback URL
- LLM API base URL and API key
- public domain and Nginx proxy settings

Do not commit production tokens, passwords, callback tokens, or deployment packages.

## Portfolio Notes

This project demonstrates:

- SaaS-style multi-tenant backend design
- WeChat private-domain sales automation
- AI reply orchestration with human-in-the-loop safety
- Real-world media message handling across browser and third-party API limits
- Vue 3 operational console development
- Spring Boot modular backend development
- Deployment and production issue troubleshooting

## License

This project is published as a personal portfolio snapshot. Check the repository license before reuse.
