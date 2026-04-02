<<<<<<< HEAD
# DearOne- 발달 치료 지원 및 관리 플랫폼 
**DearOne**은 ABA(응용행동분석) 치료 센터를 위한 통합 관리 플랫폼입니다.
치료사가 아동의 세션 데이터를 기록하고, 이를 바탕으로 AI 기반 발달 리포트를 생성하며, 센터의 전반적인 운영(아동, 치료사, 스케줄)을 효율적으로 관리할 수 있도록 지원합니다.


# 🏗 System Architecture

이 프로젝트는 **MSA(Microservice Architecture)** 지향적인 모놀리식 구조로 설계되었으며, 안정적인 데이터 처리를 위한 **Spring Boot** 백엔드와 사용자 경험을 최적화한 React 프론트엔드로 구성되어 있습니다.

|Component|Technology Stack|Description|
|------|---|---|
|**Backend**|**Java 17, Spring Boot 3, JPA**|RESTful API 서버, 비즈니스 로직 처리, 데이터 관리|
|**Database**|**PostgreSQL**|관계형 데이터 저장소 (RDBMS)|
|**Frontend**|React, TypeScript, Tailwind CSS|SPA 기반 웹 클라이언트, Shadcn UI 활용|
|**Infra**|AWS EC2, Docker, Nginx|컨테이너 기반 배포 및 운영 환경|
|**AI**|OpenAI API (GPT-4)|치료 세션 데이터 분석 및 자동 리포트 생성|



# ☕ Backend System 

백엔드 시스템은 안정성과 확장성을 고려하여 **Spring Boot** 프레임워크를 기반으로 구축되었습니다.
### 🛠 Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Persistence**: Spring Data JPA (Hibernate), PostgreSQL
- **Security**: Spring Security, JWT (Access/Refresh Token)
- **Docs**: Swagger (Springdoc OpenAPI)
- **Build**: Gradle
- **External API**: OpenAI GPT-4 (AI Report Generation)

### 🔑 Key Features & Modules
**1. Authentication & RBAC (Role-Based Access Control)**
- **JWT 기반 인증**: `JwtFilter`를 통한 Stateless 인증 처리.
- **권한 관리**: `ADMIN`(관리자), `THERAPIST`(치료사), `PARENT`(보호자) 역할에 따른 API 접근 제어.
- **초대 코드 시스템**: 센터별 고유 초대 코드를 통한 치료사 회원가입 프로세스 구현.

**2. Session & Data Tracking (핵심 기능)**
- **세션 기록**: 날짜별 치료 세션 생성 및 관리.
- **DTT(Discrete Trial Training) 데이터**: 각 목표(Goal)별 `시행 횟수(Trials)`, `성공 횟수(Successes)`, `촉구 횟수(Prompts)` 정량 데이터 수집.
- **성공률 자동 계산**: 수집된 데이터를 바탕으로 실시간 성취도(%) 계산 및 DB 저장.

**3. AI Report Generation**
- **자동화된 리포트**: 축적된 세션 데이터를 기반으로 아동의 발달 상태를 분석.
- **GPT-4 연동**: `OpenAiService`를 통해 정량적 데이터(수치)를 정성적 분석(텍스트)으로 변환하여 부모 상담용 리포트 초안 자동 생성.
- **Chart Data Provision**: 프론트엔드 시각화를 위한 JSON 형태의 차트 데이터 반환 지원.

**4. Legacy Data Migration** 
- **Excel Parsing**: 기존 오프라인/엑셀로 관리되던 치료 기록(V3, V4 양식)을 파싱하여 시스템 DB로 이관.
- **Apache POI 활용**: 대용량 엑셀 데이터의 효율적인 읽기/쓰기 및 데이터 정합성 검증 로직 구현.

**5. Dashboard & Analytics**
- **종합 대시보드**: 센터 전체의 아동 수, 금주 세션 현황, 대기 중인 리포트 등을 한눈에 파악.
- **통계 API**: 기간별, 아동별 치료 성과 추이를 시계열 데이터로 제공.


### 🎨 Frontend Overview
프론트엔드는 사용자 친화적인 UI/UX를 제공하기 위해 모던 웹 기술을 사용했습니다.

- **Stack**: React (Vite), TypeScript
- **Styling**: Tailwind CSS, Shadcn UI (Component Library)
- **State Management**: React Context API & Hooks
- **Visualization**: Recharts (치료 데이터 그래프 시각화)



### 🚀 Deployment & Operation

**Docker Build & Run**
전체 서비스는 Docker 컨테이너로 패키징되어 배포됩니다.
```
Bash
# Backend Build & Run
cd aba-back/aba-os-server
./gradlew clean build -x test
docker build -t dearone-backend .
docker run -d -p 8080:8080 --name dearone-backend dearone-backend
```

**CI/CD Pipeline**

- **GitHub Actions**: `main` 브랜치 Push 시 자동 빌드 및 배포 파이프라인 가동.
- **Process**: Code Checkout -> JDK Setup -> Gradle Build -> Docker Build & Push -> EC2 Deploy.


### 📚 API Documentation
서버가 실행 중일 때 아래 주소에서 Swagger UI를 통해 API 명세를 확인할 수 있습니다.
- **Swagger UI**: `https://dearone.kr/swagger-ui/index.html`
- **Local**: `http://localhost:8080/swagger-ui/index.html`


### 📞 Contact & Support
- **Project Lead, BackEnd Developer, DevOps Developer**: Sehyeon Kim 
- **Github** : `https://github.com/Vector1331` 
- **Email**: `tpgus4796@gmail.com`
=======
# Welcome to your Lovable project

## Project info

**URL**: https://lovable.dev/projects/REPLACE_WITH_PROJECT_ID

## How can I edit this code?

There are several ways of editing your application.

**Use Lovable**

Simply visit the [Lovable Project](https://lovable.dev/projects/REPLACE_WITH_PROJECT_ID) and start prompting.

Changes made via Lovable will be committed automatically to this repo.

**Use your preferred IDE**

If you want to work locally using your own IDE, you can clone this repo and push changes. Pushed changes will also be reflected in Lovable.

The only requirement is having Node.js & npm installed - [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating)

Follow these steps:

```sh
# Step 1: Clone the repository using the project's Git URL.
git clone <YOUR_GIT_URL>

# Step 2: Navigate to the project directory.
cd <YOUR_PROJECT_NAME>

# Step 3: Install the necessary dependencies.
npm i

# Step 4: Start the development server with auto-reloading and an instant preview.
npm run dev
```

**Edit a file directly in GitHub**

- Navigate to the desired file(s).
- Click the "Edit" button (pencil icon) at the top right of the file view.
- Make your changes and commit the changes.

**Use GitHub Codespaces**

- Navigate to the main page of your repository.
- Click on the "Code" button (green button) near the top right.
- Select the "Codespaces" tab.
- Click on "New codespace" to launch a new Codespace environment.
- Edit files directly within the Codespace and commit and push your changes once you're done.

## What technologies are used for this project?

This project is built with:

- Vite
- TypeScript
- React
- shadcn-ui
- Tailwind CSS

## How can I deploy this project?

Simply open [Lovable](https://lovable.dev/projects/REPLACE_WITH_PROJECT_ID) and click on Share -> Publish.

## Can I connect a custom domain to my Lovable project?

Yes, you can!

To connect a domain, navigate to Project > Settings > Domains and click Connect Domain.

Read more here: [Setting up a custom domain](https://docs.lovable.dev/features/custom-domain#custom-domain)
>>>>>>> origin/main
