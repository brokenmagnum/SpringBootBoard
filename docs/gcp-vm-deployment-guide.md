# GCP VM 배포 가이드 및 진행 현황

Spring Boot 게시판 프로젝트를 GCP Compute Engine 저사양 VM에 배포하기 위해 진행한 설정 내용과 배포 절차를 정리했습니다. 회사 보안 정책으로 JAR 업로드가 차단된 지점 이후 집에서 이어서 작업했고, 외부 IP 할당까지 마쳐 배포가 정상 동작하는 것을 확인했습니다.

## 목차

1. [프로젝트 및 GCP 환경 요약](#1-프로젝트-및-gcp-환경-요약)
2. [진행 현황](#2-현재까지-진행-현황)
3. [진행 중 확인한 주요 사항](#3-진행-중-확인한-주요-사항)
4. [배포 절차](#4-집에서-이어서-진행할-순서)
5. [재배포 및 운영 참고사항](#5-재배포-및-운영-참고사항)
6. [배포 전 확인한 프로젝트 설정과 보안 점검](#6-배포-전-확인한-프로젝트-설정과-보안-점검)
7. [문제 발생 시 점검표](#7-문제-발생-시-점검표)
8. [남은 과제](#8-남은-과제)

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
- **외부 IP**: VM 생성 직후에는 없었고, 이후 **임시 외부 IPv4**를 할당했습니다. 외부 IP 없이는 `8080` 접속과 VM의 인터넷 접근이 모두 불가능합니다.

## 2. 현재까지 진행 현황

- [x] GCP VM(`spring-boot-board`, `us-west1-a`) 생성 및 실행 완료
- [x] 방화벽 규칙(`spring-boot-board`) 생성 완료
  - 네트워크: `default`, 로그: `사용 안함`, 트래픽 방향: `인그레스`, 일치 시 작업: `허용`
  - 대상: `네트워크의 모든 인스턴스`
  - 소스 IPv4 범위: `0.0.0.0/0`
  - 프로토콜 및 포트: `TCP 8080`
- [x] 브라우저 SSH 접속 확인 (IAP 터널 경유로 추정, 3장 참고)
- [x] 로컬 PC에서 JAR 빌드 (`board-0.0.1-SNAPSHOT.jar`, 약 70MB)
- [x] VM에 JAR 업로드 (홈 디렉토리에서 `board-0.0.1-SNAPSHOT.jar`, `board`, `java` 항목 확인)
- [x] VM 외부 IP 할당 (기존에는 외부 IP가 없어 접속 불가, 3장 참고)
- [x] VM 내 Java 25 실행 환경 준비
- [x] 서버 실행 및 `http://<VM 외부 IP>:8080` 접속 확인 (사용자 확인 완료)
- [ ] 백그라운드 실행(`nohup`) 및 재배포 절차 정착 (이번 세션에서 실행 여부를 확인하지 못했습니다)
- [ ] 보안 보완 사항 검토 (6장, 8장 참고)

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

### 외부 IP가 없어서 생기는 문제

JAR 업로드를 마치고 `http://<VM 외부 IP>:8080`으로 접속하려는 단계에서, VM에 외부 IP가 아직 없다는 사실을 확인했습니다.

**원인 추정 근거**

- SSH 접속 직후 표시된 `Last login ... from 35.235.247.242`의 IP는 Google IAP(Identity-Aware Proxy) 대역(`35.235.240.0/20`)에 속합니다. 외부 IP 없이도 브라우저 SSH가 동작했던 이유는 IAP 터널을 거쳤기 때문으로 보입니다.
- 외부 IP가 없는 VM은 Cloud NAT를 따로 구성하지 않으면 인터넷으로 나갈 수 없습니다. 그래서 `curl`로 Temurin JRE를 내려받는 단계도 실패했을 가능성이 있습니다. `~/java` 폴더가 이미 있었지만 내용이 비어 있었을 수 있습니다.

**영향**

- 방화벽 규칙(`TCP 8080`, `0.0.0.0/0`)이 올바르게 설정되어 있어도 외부 IP가 없으면 브라우저에서 접속할 수 없습니다.
- Java 설치 파일을 내려받을 수 없습니다.

**해결: 임시 외부 IPv4 할당**

1. **Compute Engine > VM 인스턴스**에서 `spring-boot-board` 이름을 클릭합니다.
2. 상단의 **수정**을 누릅니다.
3. **네트워크 인터페이스**에서 `default`를 펼칩니다.
4. **외부 IPv4 주소**를 `없음`에서 **임시**로 변경하고 저장합니다. VM을 중지하지 않고 적용할 수 있습니다.
5. VM 상세 화면의 **외부 IP**에 주소가 표시되는지 확인합니다.

Cloud Shell에서는 아래 명령어로도 같은 작업을 할 수 있습니다.

```bash
gcloud compute instances add-access-config spring-boot-board --zone=us-west1-a
```

할당 후 SSH 창에서 아래 명령어를 실행해 VM이 인터넷에 나갈 수 있는지 확인합니다. 외부 IP가 출력되면 정상입니다.

```bash
curl -s ifconfig.me
```

**IP 요금과 고정 여부**

- 임시 IP는 VM을 중지했다가 다시 시작하면 바뀝니다. 주소를 유지하려면 **VPC 네트워크 > IP 주소**에서 고정 IP를 예약해 VM에 연결합니다. 예약한 IP를 VM에 연결하지 않고 두면 추가 요금이 발생할 수 있습니다.
- Google은 2024년부터 외부 IPv4 주소에 시간당 요금을 부과하는 것으로 알려져 있습니다. 무료 티어 e2-micro가 이 요금을 면제하는지는 이번 작업에서 확인하지 못했으므로, GCP 요금 페이지와 결제 화면에서 직접 확인해야 합니다.

### 시스템 재시작 안내 메시지

SSH 접속 시 `*** System restart required ***` 메시지가 표시됩니다. 커널 등 업데이트가 반영되려면 재부팅이 필요하다는 Ubuntu의 안내이며, 지금 배포 자체에는 영향이 없습니다. `sudo`가 막혀 있으므로 재부팅이 필요하면 GCP 콘솔에서 VM을 중지한 뒤 시작해야 합니다. 이때 `nohup`으로 실행 중인 서버는 종료되고, 고정 IP를 쓰지 않았다면 외부 IP도 바뀝니다.

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

### 2단계: 외부 IP 확인, VM SSH 접속 및 Java 25 설치 확인

> 먼저 VM에 외부 IP가 할당되어 있는지 확인합니다. 없으면 3장의 "외부 IP가 없어서 생기는 문제"를 따라 임시 외부 IPv4를 할당합니다. 외부 IP가 없으면 아래 4번의 `curl` 다운로드와 이후 브라우저 접속이 모두 실패합니다.

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
   - `java: command not found`가 나오면 PATH만 빠진 경우일 수 있습니다. `~/java/bin/java -version`이 동작하는지 확인하고, 동작하면 `export PATH="$HOME/java/bin:$PATH"`를 실행합니다.
   - `~/java/bin/java`도 없으면 `ls ~/java`로 폴더 내용을 확인하고, 비어 있으면 위 설치 명령어를 다시 실행합니다.

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
4. 이동 결과를 확인합니다.
   ```bash
   ls ~/board
   ```

> 업로드 직후 홈 디렉토리(`~`)에서 `ls`를 실행하면 `board`, `board-0.0.1-SNAPSHOT.jar`, `java`가 보이는 것이 정상입니다. 이번 배포에서는 `local.mv.db`를 올리지 않았으므로 서버 최초 실행 시 새 DB 파일이 생성됩니다. 기존 데이터가 필요하면 서버를 처음 실행하기 전에 이 파일을 `~/board`에 먼저 두어야 합니다.

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
   - 외부 IP 칸이 비어 있으면 아직 할당되지 않은 것입니다. 3장의 안내에 따라 먼저 할당합니다.
   - SSH 창에서 `curl -s ifconfig.me`를 실행해도 VM의 외부 IP를 확인할 수 있습니다.
3. 웹 브라우저에서 아래 주소로 접속해 게시판 동작을 확인합니다.
   ```text
   http://<VM 외부 IP>:8080
   ```
   주소는 `https`가 아니라 `http`로 시작해야 합니다. 앱이 HTTPS를 지원하도록 설정되어 있지 않기 때문입니다.
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

## 6. 배포 전 확인한 프로젝트 설정과 보안 점검

배포에 앞서 `board/build.gradle`, `board/src/main/resources/application.properties`, 로컬 빌드 산출물을 읽기 전용으로 확인했습니다. 파일은 수정하지 않았습니다.

### 문서 내용과 일치한 항목

| 항목 | 확인 결과 |
| --- | --- |
| Spring Boot 버전 | `4.2.0` |
| Java toolchain | `JavaLanguageVersion.of(25)` |
| 아티팩트 버전 | `0.0.1-SNAPSHOT` |
| DB 접속 URL | `jdbc:h2:./local` (실행한 디렉토리에 `local.mv.db` 생성 또는 재사용) |
| 스키마 정책 | `spring.jpa.hibernate.ddl-auto=update` |
| 첨부파일 제한 | 파일당 1MB, 요청당 4MB |

`jdbc:h2:./local`은 실행 위치를 기준으로 하는 상대 경로입니다. 그래서 반드시 `cd ~/board`로 이동한 뒤 서버를 실행해야 DB 파일이 항상 같은 위치에 만들어집니다.

### 로컬 빌드 산출물

| 파일 | 크기 | 수정 시각 |
| --- | --- | --- |
| `board/build/libs/board-0.0.1-SNAPSHOT.jar` | 약 70MB | 2026-11-28 10:05 |
| `board/local.mv.db` | 약 132KB | 2026-11-27 20:53 |

JAR가 최신 소스로 빌드되었는지는 파일 시각만으로 확인할 수 없습니다. 코드를 수정한 뒤에는 5장의 재배포 순서대로 JAR를 다시 빌드해 올립니다.

### 보안 관련 설정

| 설정 | 현재 값 | 영향과 권장 조치 |
| --- | --- | --- |
| `spring.h2.console.enabled` | `true` | 실행 인자 `--spring.h2.console.enabled=false`를 빠뜨리면 `/h2-console`이 외부에 공개됩니다. 모든 실행 명령어에 이 인자를 포함합니다. |
| H2 접속 계정 | `sa` / 빈 비밀번호 | H2 콘솔이 열려 있으면 인증 없이 DB에 접근할 수 있습니다. 위 인자로 콘솔을 끄고, 가능하면 비밀번호를 설정합니다. |
| `spring.security.user.name`, `password` | `user` / `password` | 앱이 별도의 `UserDetailsService`로 인증한다면 영향이 없습니다. 그렇지 않으면 이 계정으로 외부에서 로그인할 수 있습니다. 이번 점검에서는 보안 설정 클래스를 읽지 않았으므로 확인이 필요합니다. |
| 방화벽 소스 범위 | `0.0.0.0/0`, `TCP 8080` | 누구나 접속할 수 있는 상태입니다. 개인 테스트라면 본인 IP로 제한하는 방안을 검토합니다. |
| 전송 구간 | HTTP | 로그인 정보가 암호화되지 않고 전송됩니다. 장기 운영 전에 HTTPS를 적용합니다. |

## 7. 문제 발생 시 점검표

서버 실행이나 접속 중 문제가 생기면 아래 표에서 증상에 해당하는 항목을 확인합니다.

| 증상 | 원인과 조치 |
| --- | --- |
| `sudo: I'm sorry gaofar. I'm afraid I can't do that` | 현재 계정에 `apt install` 권한이 없습니다. 3장의 방법대로 `~/java`에 JRE를 직접 설치합니다. |
| `java: command not found` | 설치가 안 되었거나 PATH가 설정되지 않았습니다. `~/java/bin/java -version`으로 확인하고 PATH를 추가합니다. |
| `curl` 다운로드 실패 또는 응답 없음 | 외부 IP가 없어 인터넷에 나가지 못하는 상태일 수 있습니다. 외부 IP를 할당한 뒤 `curl -s ifconfig.me`로 확인합니다. |
| `UnsupportedClassVersionError` | Java 버전이 25 미만입니다. Temurin 25를 다시 설치합니다. |
| `Killed` | 1GB RAM에서 메모리가 부족해 프로세스가 종료되었습니다. `-Xmx`를 384m 정도로 낮추거나 스왑 추가를 검토합니다. |
| `Port 8080 was already in use` | 이미 실행 중인 프로세스가 있습니다. `ps aux \| grep board`로 확인하고 `kill <PID>`로 종료합니다. |
| 서버 로그는 정상인데 브라우저 접속 불가 | 외부 IP 할당 여부, 주소가 `http://<외부IP>:8080`인지, 방화벽 규칙의 `TCP 8080` 허용 여부를 순서대로 확인합니다. VM을 재시작했다면 외부 IP가 바뀌었을 수 있습니다. |
| 브라우저에서 `https`로 접속 시 오류 | 앱은 HTTP만 지원합니다. `http://`로 접속합니다. |

## 8. 남은 과제

우선순위가 높은 항목부터 나열했습니다.

1. **백그라운드 실행 확인**: 5단계의 `nohup` 명령어로 실행하고, SSH 창을 닫은 뒤에도 접속이 유지되는지 확인합니다.
2. **보안 설정 점검**: 보안 설정 클래스를 읽어 `user` / `password` 기본 계정의 사용 여부를 확인합니다. H2 콘솔 비활성화는 `application.properties`에 반영하거나 운영용 프로필로 분리하는 방법을 검토합니다.
3. **외부 IP 정책 결정**: 임시 IP를 계속 쓸지, 고정 IP를 예약할지 정합니다. 요금은 GCP 결제 화면에서 확인합니다.
4. **자동 실행 구성**: `systemd` 서비스로 등록해 VM 재부팅 후에도 서버가 자동으로 실행되게 합니다. 현재 `sudo`가 제한되어 있어, 사용자 단위 서비스(`systemctl --user`) 사용 가능 여부를 먼저 확인해야 합니다.
5. **HTTPS 적용**: 도메인을 준비한 뒤 Nginx 리버스 프록시와 인증서를 구성합니다.
6. **데이터 백업**: 게시글과 회원 데이터가 `~/board/local.mv.db` 한 파일에 있습니다. 재배포 전에 이 파일을 복사해 두면 데이터 유실을 막을 수 있습니다.

## 개정이력

- `2026-11-27 10:22:52` KST — GCP VM 배포 진행 현황, 트러블슈팅 내용, 집에서 이어서 수행할 단계별 배포 가이드를 최초 작성했습니다.
- `2026-11-28 10:33:28` KST — 배포 완료(외부 IP 접속 확인) 내용을 반영해 문서를 갱신했습니다. 목차를 추가하고, 진행 현황 체크리스트를 갱신했습니다. 외부 IP 미할당 문제와 해결 방법, 시스템 재시작 안내 메시지, 배포 절차의 보완 사항을 추가했습니다. 프로젝트 설정과 보안 점검 결과(6장), 문제 발생 시 점검표(7장), 남은 과제(8장)를 새로 정리했습니다.
