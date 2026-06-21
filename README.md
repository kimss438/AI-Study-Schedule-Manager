# AI Study Schedule Manager

Java Swing 기반 학습 일정 관리 프로그램입니다.

## 주요 기능
- 학습 할 일 등록
- 우선순위 계산 및 정렬
- 진행률 수정 및 완료 처리
- 파일 저장 및 불러오기
- OpenAI API 기반 학습 계획 생성

## AI 기능 사용 방법

AI 학습 계획 생성 기능을 사용하려면 OpenAI API Key를 환경변수로 설정해야 합니다.

### macOS / Linux

터미널에서 아래 명령어를 입력합니다.

```bash
export OPENAI_API_KEY="본인의_API_KEY"
```

환경변수 설정 후 프로그램을 실행합니다.

```bash
javac gui.java
java gui
```

API Key가 설정되지 않은 경우에도 할 일 등록, 우선순위 계산, 진행률 수정, 저장 및 불러오기 기능은 정상적으로 사용할 수 있습니다. 다만 AI 학습 계획 생성 기능은 OpenAI API Key가 필요합니다.

API Key는 코드에 직접 입력하지 않고 환경변수로 관리합니다. 따라서 API Key는 GitHub 저장소에 업로드되지 않습니다.


## 실행 방법
```bash
javac gui.java
java gui
