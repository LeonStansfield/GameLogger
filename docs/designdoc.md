# App Design Doc: GameLogger
## 1. App overview
### 1.1 Idea

GameLogger is an Android game diary app. The goal is a tool for users to track, rate, and review games they've played.
### 1.2 The plan

The core loop is: Find a game → Log/review it → See it in your diary. Using the modern Android stack with Android Studio, Kotlin, Jetpack Compose, and a clean MVVM architecture.

### 1.3 Similar existing apps

 - IGDB — a massive database, but clunky for just logging personal reviews.

 - Backloggd — great, but heavily social and can have a complex UX.

Ours will be a simple game logging app with a diary, backlog and user reviews and we can add more complexity as we go.

### 1.4 Target user (persona)

Persona: Alex

 - Who: Plays a few games a week. Likes to keep a personal record of his thoughts.

 - Goal: Wants a super-quick way to log a game right after playing it. Needs a simple diary and a backlog.

 - Pain point: Finds other apps too complex (Backloggd has a lot of social features). He just wants a private logbook.

(Mark scheme asks for personas)

## 2. Requirements
### 2.1 Functional requirements (MVP)

Here's the feature set expressed as user stories:

FR1 — Game search & discovery

 - As a user, I want to search for any game by title.

 - Details: We'll hit the IGDB API from a search screen. The home screen will show a "Trending" list from the API for easy discovery.

FR2 — Game details screen

 - As a user, I want to see key details of a game before I log it.

 - Details: Pull and display the cover art, title, release year, summary, developer, publisher, and IGDB rating.

FR3 — Game logging & reviewing

 - As a user, I want to log a game with a star rating and a short review.

 - Details: A simple dialog with a 5-star selector (supporting half-stars) and an optional text field.

FR4 — Diary screen (my reviews)

 - As a user, I want to see a list of all the games I've logged.

 - Details: A screen listing all logged games. Each item should show the cover art, title, and the user's rating. Tapping an entry should show the full review text. Needs edit and delete functionality.

FR5 — Backlog

 - As a user, I want to add games to a backlog for later.

 - Details: Simple add/remove functionality from the Game Details screen. A dedicated tab will show the list.

FR6 — Local persistence

 - As a user, I want my data saved on my device so it's always available.

 - Details: Use a Room database for all reviews and backlog items.

### 2.2 Non-functional requirements (quality & performance)

Performance

 - UI must be smooth (no jank).

 - API calls need loading indicators.

 - Cold start should be under 2 seconds.

Usability

 - Follow Material Design guidelines for accessibility (touch targets, contrast, etc.).

 - The main "find → log" flow should be fast and intuitive.

Scalability

 - The MVVM architecture should be clean so we can add stretch goals later without a massive refactor.

Reliability

 - Handle no-network scenarios gracefully. Show a message, don't crash.

### 2.3 Scope

IN: Everything listed in FR1–FR6.

OUT: User accounts, social features, offline mode (can be added later).

## 3. Tech & architecture
### 3.1 Tech stack

 - Language: Kotlin

 - UI: Jetpack Compose

 - Architecture: MVVM

 - API client: Ktor (IGDB)

 - Database: Room

 - Dependency injection: Hilt (TBD)

### 3.2 Data models

We'll store only user-generated data and the igdb_game_id. We'll fetch game details from the API on demand to keep them up-to-date.

ReviewEntity (Room table)

 - igdb_game_id: Int (PK)

 - user_rating: Float (supports half-stars, e.g. 3.5)

 - review_text: String? (nullable)

 - date_logged: Long (Unix timestamp)

BacklogEntity (Room table):

 - igdb_game_id: Int (PK)

 - date_added: Long (Unix timestamp)

## 4. UI / UX plan
### 4.1 App flow

The app will use a standard bottom navigation bar with four main tabs:

 - Discover

 - Search

 - Diary

 - Backlog

Example flow:

 - User opens app, lands on Discover.

 - Taps Search, types "Elden Ring", taps the result.

 - On the Game Details screen, taps "Log Game".

 - A dialog pops up. User gives it 5 stars, writes a review, hits "Save".

 - The app saves the log and navigates them to the Diary tab, showing the new entry.

### 4.2 Wireframes plan

Create low-fi wireframes for every screen (labelled boxes and text):

WF-01: Discover — grid of cover art, top app bar, bottom nav.

WF-02: Search — text input at top, list of results below.

WF-03: Game Details — cover art, text info, "Log" and "Add to Backlog" buttons.

WF-04: Log Game Dialog — modal overlay with stars, text input, Save/Cancel.

WF-05: Diary — scrolling list of logged games (cover art, title, rating).

### 4.3 Visual design plan

Once the wireframes are set, create high-fidelity mockups (composites) that translate directly into Jetpack Compose code.

 > Gracie can do design?

### 5. Stretch goals

Diary enhancements: add a "date played" field and a timeline view.

User stats: a profile page with simple stats like "games finished this year".

Custom lists: let users create their own named lists (e.g., "Best RPGs").

User accounts: basic sign-in with Google for cloud sync (Firebase Auth + Firestore).