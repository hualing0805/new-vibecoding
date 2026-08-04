# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

delegated: Java Spring Boot 3 for the main backend, Vue 3 + Vite + TypeScript for the browser app, Python FastAPI for the Agent service, and local MySQL 8 for persistence. This follows the user's requested stack and keeps each runtime in its own service boundary.

## Users

Primary users are forum members who want to browse discussions, publish posts, reply to others, collect useful threads, and use AI while writing or finding information.

Secondary users are moderators and administrators who need a clear queue for reports, content status, user state, and AI-assisted risk hints.

## Product Purpose

ThreadMind is a network forum with AI assistance built into normal discussion workflows. The first useful version should let users register, log in, browse sections, publish and reply to posts, and call AI for polishing, tag suggestions, summaries, and site-aware Q&A.

Success means the forum remains usable even when the Agent service is offline, while AI features make writing, reading, retrieval, and moderation faster.

## Positioning

The product treats AI as a bounded assistant inside a forum, not as the owner of business state. Java keeps authentication, permissions, transactions, and audit records; Python Agent can reason and generate suggestions only through controlled interfaces.

## Operating Context

The product is developed locally as a full-stack practice project. The expected local topology is Vue on port 5173, Java on port 8080, Python Agent on port 8000, and MySQL on port 3306.

Common workflows include reading a thread list, opening a topic, replying, drafting a post with AI help, asking the assistant about site content, and handling reports from an admin surface.

## Capabilities and Constraints

Confirmed capabilities for the initial build include account authentication, categories, topics, replies, reactions, favorites, notifications, reports, moderation records, AI chat, AI polishing, tag suggestions, topic summaries, and moderation checks.

The Agent service should not write directly to MySQL. It should call Java-controlled tools or receive sanitized context from the Java gateway.

The first version may use synthetic seed data and mock model responses when no model API key is configured.

Open decisions: final product name, exact visual identity, whether to add Redis in the first real implementation, and whether AI conversations should stream token-by-token in the first release.

## Brand Commitments

Working name: ThreadMind. The name is provisional and can be changed before public release.

The product voice should feel like a practical engineering tool for a forum community: clear, compact, and calm. It should avoid exaggerated AI claims.

## Evidence on Hand

The repository currently contains the project说明书 at `docs/forum-ai-project-spec.md`. No real users, screenshots, production data, customers, pricing, or external claims exist yet, so future UI must not fabricate them.

## Product Principles

Forum first: browsing, posting, replying, and moderation must work without AI.

Bounded AI: Agent output is advice or draft material unless Java confirms and writes the business change.

Traceable decisions: important writes, moderation actions, and AI tool calls carry IDs and audit records.

Local-first development: the project should be easy to run on one machine before adding external infrastructure.

Clean service boundaries: each runtime owns a clear slice of the system and communicates through explicit contracts.

## Accessibility & Inclusion

The web interface should support keyboard navigation, visible focus states, readable contrast, responsive layouts, and plain labels for core actions.

