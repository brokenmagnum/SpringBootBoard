# `gradlew` 실행 권한 오류 해결 및 `bootJar` 생성 결과

Linux 환경에서 `./gradlew bootJar` 실행 시 발생한 `허가 거부` 오류의 원인과 조치 방법, 빌드 후 생성된 실행용 JAR 파일의 위치 및 재발 방지 참고사항을 정리했습니다.

## 1. 오류 증상

`board` 디렉토리에서 실행용 JAR을 빌드하기 위해 `./gradlew bootJar`를 실행했을 때 권한 거부 오류가 발생했습니다. `sudo`를 붙여 실행해도 동일하게 실행이 차단되었습니다.

```text
$ ./gradlew bootJar
bash: ./gradlew: 허가 거부

$ sudo ./gradlew bootJar
sudo: cannot execute '/home/user/workspace/SpringBootBoard/board/gradlew': 허가 거부 (os error 13)
```

## 2. 원인 분석

[gradlew](../board/gradlew) 파일 상태와 마운트 옵션, Git 추적 모드를 점검해 원인을 확인했습니다.

| 점검 항목 | 확인 결과 | 판단 |
| --- | --- | --- |
| 파일 권한 (`ls -la`) | `-rw-r--r--` (`644`) | 실행 권한(`x`) 누락 (직접 원인) |
| 파일 형식 (`file`) | `POSIX shell script, Unicode text, UTF-8 text executable` | LF 줄바꿈 및 스크립트 형식 정상 |
| 마운트 옵션 (`findmnt`) | `ext4`, `rw,relatime` | `noexec` 제한 없음 |
| Git 인덱스 모드 (`git ls-files -s`) | `100644` | 실행 권한 없이 커밋된 상태 |

리눅스에서는 파일 자체에 실행 권한(`x`) 비트가 설정되어 있지 않으면 `sudo`로 관리자 권한을 얻더라도 스크립트를 직접 실행할 수 없습니다. 또한 Git 인덱스에 `100644`로 기록되어 있어, 다른 리눅스 환경에서 저장소를 새로 클론할 때도 실행 권한이 빠진 상태로 내려받아집니다.

## 3. 조치 방법 및 빌드 결과

### 실행 권한 부여

`chmod +x` 명령으로 `board/gradlew`에 실행 권한을 추가했습니다.

```bash
chmod +x board/gradlew
```

권한 변경 후 파일 상태는 `-rwxr-xr-x` (`755`)가 되었으며, 이후 `board` 디렉토리에서 `sudo` 없이 바로 빌드할 수 있습니다.

```bash
cd board
./gradlew bootJar
```

### 빌드 기준 정보

- **Gradle Wrapper**: Gradle `9.9.2` ([gradle-wrapper.properties](../board/gradle/wrapper/gradle-wrapper.properties))
- **프레임워크 및 Java**: Spring Boot `4.2.0`, Java `25` ([build.gradle](../board/build.gradle))
- **아티팩트 버전**: `0.0.1-SNAPSHOT`

## 4. 생성된 JAR 파일 위치

`./gradlew bootJar` 빌드가 완료되면 실행용 JAR 파일은 `board/build/libs/` 디렉토리에 생성됩니다.

- **파일 경로**: `board/build/libs/board-0.0.1-SNAPSHOT.jar`
- **확인된 크기 및 생성 시각**: `67MB` / `2026-11-27 10:01` (KST)

터미널에서 생성 결과를 확인할 때는 아래 명령어를 사용합니다.

```bash
ls -lh board/build/libs/
```

생성된 JAR 파일을 GCP VM에 업로드하고 실행하는 절차는 [gcp-vm-deployment-guide.md](./gcp-vm-deployment-guide.md)에 정리되어 있습니다.

## 5. 참고 및 재발 방지 사항

### `sudo`로 Gradle을 실행하지 않기

`./gradlew`를 `sudo`로 실행하면 `board/build/`나 `~/.gradle/` 아래에 `root` 소유의 파일과 캐시가 생성됩니다. 이후 일반 사용자 계정으로 다시 빌드할 때 파일 쓰기 권한 충돌이 발생할 수 있으므로, 실행 권한만 부여한 뒤 일반 사용자 권한으로 실행하는 것이 안전합니다.

### Git 저장소에 실행 권한 반영 (선택)

현재 워킹 트리의 `board/gradlew`에는 실행 권한(`755`)이 부여되어 있지만, Git 인덱스에는 아직 `100644`로 남아 있습니다. 다른 Linux/macOS 환경에서 저장소를 새로 클론할 때 같은 문제가 재발하지 않도록 하려면 아래 명령어로 Git 인덱스에 실행 권한(`100755`)을 반영한 뒤 커밋하면 됩니다.

```bash
git update-index --chmod=+x board/gradlew
```

## 개정이력

- `2026-11-27 10:26:24` KST — `gradlew` 실행 권한 오류 원인, 조치 방법, `bootJar` 생성 경로 및 재발 방지 참고사항을 최초 작성했습니다.
