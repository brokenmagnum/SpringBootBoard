# GCP VM 배포 가이드 및 진행 현황

Spring Boot 게시판 프로젝트를 GCP Compute Engine 저사양 VM에 배포하기 위해 진행한 설정 내용과, 회사 보안 정책으로 JAR 업로드가 차단된 지점 이후 집에서 바로 이어서 진행할 절차를 정리했습니다.

## 1. 프로젝트 및 GCP 환경 요약

### 프로젝트 기준 정보

- **프레임워크 및 빌드 도구**: Spring Boot 4.2.0, Gradle (`bootJar`) [build.gradle](../board/build.gradle)
- **Java 버전**: Java 25 (`JavaLanguageVersion.of(25)`)
- **배포 파일**: `board/build/libs/board-0.0.1-SNAPSHOT.jar` (`plain.jar`가 아닌 용량이 큰 실행용 JAR 사용)
- **데이터베이스**: H2 파일 DB (`jdbc:h2:./local`, `local.mv.db`) [application.properties](../board/src/main/resources/application.properties)
  - 첨부파일도 DB에 `byte[]`로 저장되므로 별도의 DB 서버나 업로드 디렉토리 설정 없이 바로 실행할 수 있습니다.
- **기본 포트**: `8080`

### 확인된 GCP VM 환경

- **GCP 프로젝트**: `SprinBootBoard`
- **VM 인스턴스 이름**: `spring-boot-board`
- **영역**: `us-west1-a` (무료 티어 대상 리전)
- **운영체제**: Ubuntu 26.04.1 LTS (`x86_64`)
- **SSH 사용자 계정**: `gaofar`

## 2. 현재까지 진행 현황

- [x] GCP VM(`spring-boot-board`, `us-west1-a`) 생성 및 실행 완료
- [x] 방화벽 규칙(`spring-boot-board`) 생성 완료
  - 네트워크: `default`, 로그: `사용 안함`, 트래픽 방향: `인그레스`, 일치 시 작업: `허용`
  - 대상: `네트워크의 모든 인스턴스`
  - 소스 IPv4 범위: `0.0.0.0/0`
  - 프로토콜 및 포트: `TCP 8080`
- [x] 브라우저 SSH 접속 확인
- [ ] VM 내 Java 25 설치 완료 여부 확인 (아래 `sudo` 제약 사항 참고)
- [ ] 로컬 PC에서 JAR 빌드 및 VM 업로드 (회사 보안으로 업로드 차단되어 중단)
- [ ] 서버 실행, 외부 IP 접속 확인 및 백그라운드 실행

## 3. 진행 중 확인한 주요 사항

### 로컬 빌드 후 업로드 방식 사용

저사양 VM(`e2-micro`, RAM 1GB) 안에서 `./gradlew`로 직접 빌드하면 메모리 부족으로 인스턴스가 멈출 수 있습니다. 로컬 PC에서 실행용 JAR을 먼저 만든 뒤 VM으로 업로드하고, 실행 시 JVM 힙 메모리를 `-Xms256m -Xmx512m`으로 제한하는 방식을 사용합니다.

### 방화벽 대상 태그 주의

방화벽 규칙의 `대상`을 `지정된 대상 태그`로 두고 태그에 `*`를 입력하는 방식은 GCP에서 와일드카드로 동작하지 않습니다. VM이 하나뿐인 현재 환경에서는 `대상`을 `네트워크의 모든 인스턴스`로 설정하는 것이 가장 간단하며, 이 방식으로 설정을 마쳤습니다.

### Adoptium JDK와 OpenJDK JRE 호환성

로컬 개발 환경의 Adoptium(Temurin) JDK 25와 서버의 OpenJDK 25 JRE는 모두 동일한 OpenJDK 25 표준을 따르므로 메이저 버전(25)만 일치하면 문제없이 호환됩니다. 서버에서는 컴파일 없이 JAR 실행만 수행하므로 JDK 대신 JRE(GUI가 제외된 `headless` 포함)로 충분합니다.

### `sudo apt install` 실행 거부 오류

VM에서 `sudo apt install -y openjdk-25-jre-headless` 실행 시 아래 오류가 발생했습니다.

```text
sudo: I'm sorry gaofar. I'm afraid I can't do that
```

Ubuntu 26.04의 `sudo`가 현재 계정의 `apt install` 권한을 허용하지 않아 발생한 메시지입니다(`sudo -l`로 허용된 권한 확인 가능). 시스템 전역 설치 대신 사용자 홈 디렉토리(`~/java`)에 Adoptium(Temurin) 25 JRE를 직접 내려받아 압축 해제하면 `sudo` 권한 없이도 동일하게 실행할 수 있습니다.

## 4. 집에서 이어서 진행할 순서

### 1단계: 로컬 PC에서 실행용 JAR 빌드

프로젝트의 `board` 폴더에서 실행용 JAR 파일을 생성합니다.

- **Linux / macOS**:
  ```bash
  cd SpringBootBoard/board
  ./gradlew bootJar
  ```
- **Windows (PowerShell / CMD)**:
  ```powershell
  cd SpringBootBoard\board
  .\gradlew.bat bootJar
  ```

빌드가 완료되면 `board/build/libs/board-0.0.1-SNAPSHOT.jar` 파일이 생성됩니다.

### 2단계: VM SSH 접속 및 Java 25 설치 확인

1. GCP 콘솔에서 **Compute Engine > VM 인스턴스**로 이동합니다.
2. `spring-boot-board` 행 오른쪽의 **SSH** 버튼을 눌러 브라우저 터미널을 엽니다.
3. 먼저 Java 25가 이미 설치되어 있는지 확인합니다.
   ```bash
   java -version
   ```
4. 설치되어 있지 않다면 아래 명령어로 `sudo` 없이 Adoptium(Temurin) 25 JRE를 홈 디렉토리에 설치합니다.
   ```bash
   mkdir -p ~/java ~/board
   curl -fSL "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jre/hotspot/normal/eclipse" -o ~/java/temurin25.tar.gz
   tar -xzf ~/java/temurin25.tar.gz -C ~/java --strip-components=1
   rm ~/java/temurin25.tar.gz
   echo 'export PATH="$HOME/java/bin:$PATH"' >> ~/.bashrc
   source ~/.bashrc
   java -version
   ```
   `openjdk version "25..."`이 출력되면 정상 설치된 것입니다.

> 참고: 만약 `sudo` 권한이 해결되어 스왑 메모리(2GB)를 추가할 수 있다면 아래 명령어를 최초 1회 실행해 두면 1GB RAM 환경에서 더 안정적으로 동작합니다. 권한이 없다면 건너뛰고 바로 다음 단계로 진행합니다.
> ```bash
> sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
> ```

### 3단계: JAR 파일 VM으로 업로드

1. 브라우저 SSH 창 우측 상단의 **파일 업로드** 버튼을 클릭합니다.
2. 1단계에서 생성한 `board/build/libs/board-0.0.1-SNAPSHOT.jar` 파일을 선택해 업로드합니다.
   - 기존 로컬 게시글과 회원 데이터를 그대로 옮기고 싶다면 `board/local.mv.db` 파일도 함께 업로드합니다. 올리지 않으면 서버 최초 실행 시 새 DB 파일이 자동 생성됩니다.
3. 홈 디렉토리(`~`)에 올라온 파일을 `~/board` 폴더로 이동합니다.
   ```bash
   mkdir -p ~/board
   mv ~/board-0.0.1-SNAPSHOT.jar ~/board/
   ```
   (`local.mv.db`도 올렸다면 `mv ~/local.mv.db ~/board/`를 함께 실행합니다.)

### 4단계: 테스트 실행 및 브라우저 접속 확인

1. 아래 명령어로 서버를 포그라운드에서 먼저 실행해 오류 여부를 확인합니다.
   ```bash
   cd ~/board
   java -Xms256m -Xmx512m -jar board-0.0.1-SNAPSHOT.jar --spring.h2.console.enabled=false
   ```
   - `-Xms256m -Xmx512m`: 1GB RAM VM에서 메모리 사용량을 제한합니다.
   - `--spring.h2.console.enabled=false`: 소스 코드를 수정하지 않고도 외부 공개 환경에서 `/h2-console` 접근을 비활성화합니다.
2. 로그에 `Started BoardApplication in ... seconds`가 출력되면 VM의 **외부 IP**를 확인합니다.
   - **Compute Engine > VM 인스턴스** 목록 표를 오른쪽으로 스크롤하거나, `spring-boot-board` 이름을 클릭해 상세 화면에서 외부 IP를 확인할 수 있습니다.
3. 웹 브라우저에서 아래 주소로 접속해 게시판 동작을 확인합니다.
   ```text
   http://<VM 외부 IP>:8080
   ```
4. 정상 접속이 확인되면 SSH 창에서 `Ctrl + C`를 눌러 테스트 실행을 종료합니다.

### 5단계: 백그라운드 실행 (SSH 창을 닫아도 유지)

```bash
cd ~/board
nohup java -Xms256m -Xmx512m -jar board-0.0.1-SNAPSHOT.jar --spring.h2.console.enabled=false > app.log 2>&1 &
```

운영 시 자주 사용하는 명령어는 다음과 같습니다.

| 목적 | 명령어 |
| --- | --- |
| 실시간 로그 확인 (`Ctrl + C`로 종료) | `tail -f ~/board/app.log` |
| 실행 중인 서버 PID 확인 | `ps aux \| grep board-0.0.1-SNAPSHOT.jar` |
| 서버 종료 | `kill <PID>` |

## 5. 재배포 및 운영 참고사항

### 코드 수정 후 재배포 순서

1. 로컬 PC에서 `./gradlew bootJar`로 새 JAR 파일 생성
2. VM에서 기존 프로세스 확인(`ps aux | grep board-0.0.1-SNAPSHOT.jar`) 후 종료(`kill <PID>`)
3. 새 `board-0.0.1-SNAPSHOT.jar` 업로드 후 `~/board/`로 이동(기존 파일 덮어쓰기)
4. 5단계의 `nohup` 명령어로 서버 재실행

### 기타 참고사항

- **임시 외부 IP 변경**: VM을 중지했다가 다시 시작하면 임시 외부 IP가 변경될 수 있습니다.
- **추후 개선(선택)**: 기본 배포에 익숙해진 뒤에는 `systemd` 서비스 등록(VM 재부팅 시 자동 실행), Nginx 리버스 프록시 연동(80/443 포트 및 도메인/HTTPS 적용)을 순서대로 검토할 수 있습니다.

## 개정이력

- `2026-11-27 10:22:52` KST — GCP VM 배포 진행 현황, 트러블슈팅 내용, 집에서 이어서 수행할 단계별 배포 가이드를 최초 작성했습니다.
