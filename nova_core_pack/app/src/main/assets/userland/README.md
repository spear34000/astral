# Node.js Userland Setup Guide

이 디렉토리에는 PRoot 기반 Node.js 유저랜드를 실행하기 위한 필수 파일들이 위치합니다.

## 필요한 파일

### 1. PRoot Binary (proot-arm64 또는 proot-armv7)

**소스:**
- [Termux PRoot Releases](https://github.com/termux/proot/releases)
- 또는 `proot-distro`에서 추출

**다운로드 방법:**
```bash
# ARM64용
wget https://github.com/termux/proot/releases/download/v<version>/proot-arm64 -O proot-arm64
chmod +x proot-arm64

# ARMv7용 (선택사항)
wget https://github.com/termux/proot/releases/download/v<version>/proot-armv7 -O proot-armv7
chmod +x proot-armv7
```

**위치:**
- `app/src/main/assets/userland/proot-arm64`
- `app/src/main/assets/userland/proot-armv7` (선택사항)

---

### 2. Alpine Linux Rootfs (alpine-rootfs.tar.xz)

**소스:**
- [Alpine Linux Downloads](https://alpinelinux.org/downloads/)
- Alpine Minirootfs recommended (용량 최소화)

**다운로드 방법:**
```bash
# Alpine 3.19 ARM64 예시
wget https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.0-aarch64.tar.gz

# tar.xz로 재압축 (용량 절약)
tar -xzf alpine-minirootfs-3.19.0-aarch64.tar.gz
tar -cJf alpine-rootfs.tar.xz *
```

**위치:**
- `app/src/main/assets/userland/alpine-rootfs.tar.xz`

**예상 크기:**
- 압축 후: ~5-10MB (Alpine Minirootfs)
- 압축 해제 후: ~20-30MB

---

### 3. Node.js 설치 (자동)

Node.js는 앱 초기 실행 시 **자동으로 설치**됩니다:
```bash
apk add --no-cache nodejs npm
```

또는 미리 Node.js가 설치된 rootfs를 만들 수도 있습니다:
```bash
# PRoot 환경에서 직접 설치
proot -r ./rootfs /bin/sh
apk add --no-cache nodejs npm
exit

# 압축
tar -cJf alpine-nodejs-rootfs.tar.xz -C rootfs .
```

---

## 빠른 설정 (권장)

### 옵션 A: 기본 rootfs 사용 (작은 용량)
1. Alpine Minirootfs 다운로드
2. tar.xz로 압축
3. `alpine-rootfs.tar.xz`로 저장
4. 앱 실행 시 Node.js 자동 설치

### 옵션 B: Node.js 사전 설치 (빠른 실행)
1. Alpine rootfs 압축 해제
2. PRoot로 rootfs에 진입
3. `apk add nodejs npm` 실행
4. 압축하여 `alpine-nodejs-rootfs.tar.xz`로 저장

---

## 다운로드 스크립트 (준비 중)

향후 버전에서는 **첫 실행 시 자동 다운로드** 기능을 추가할 예정입니다:
- PRoot 바이너리: GitHub Releases에서 다운로드
- Rootfs: CDN에서 다운로드 또는 작은 bootstrap 파일로 시작

---

## 용량 최적화 팁

1. **Alpine Minirootfs 사용**: Debian 대신 Alpine 사용 시 용량 절약 (200MB → 10MB)
2. **불필요한 패키지 제거**: `apk del` 명령으로 불필요한 패키지 삭제
3. **tar.xz 압축**: gzip 대신 xz 압축 사용 시 ~30% 추가 절약

---

## 테스트 방법

PRoot 및 rootfs가 제대로 작동하는지 확인:

```bash
# PRoot 테스트
./proot-arm64 -r ./rootfs /bin/sh -c "echo Hello from PRoot"

# Node.js 테스트
./proot-arm64 -r ./rootfs /bin/sh -c "node -e 'console.log(\"Node.js works!\")'"
```

---

## 문제 해결

### PRoot 실행 권한 오류
```bash
chmod +x proot-arm64
```

### Rootfs 압축 오류
```bash
# tar.xz 파일 검증
xz -t alpine-rootfs.tar.xz
```

### Node.js 설치 실패
```bash
# Alpine에서 apk 업데이트
apk update
apk add nodejs npm --no-cache
```

---

## 참고 자료

- [PRoot GitHub](https://github.com/termux/proot)
- [Alpine Linux](https://alpinelinux.org/)
- [Termux PRoot Distro](https://github.com/termux/proot-distro)
