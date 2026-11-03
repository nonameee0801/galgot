# 갈곳 배포 방법

서버 1대에서 Docker Compose로 3개를 띄운다.

| 이름 | 하는 일 | 바깥 포트 |
|---|---|---|
| web (Caddy) | HTTPS 인증서 자동 발급, React 화면, `/api`·`/oauth2`·`/login/oauth2` 를 app으로 넘김 | 80, 443 |
| app (Spring Boot) | API, 카카오·이메일 로그인 | 없음 |
| db (MySQL 8.4) | 데이터(Docker 볼륨 `db-data`) | 없음 |

화면과 API가 같은 주소(같은 출처)라 세션 쿠키 + SameSite=Lax 방식을 그대로 쓴다.

현재 배포: AWS EC2 시드니(ap-southeast-2), t4g.small(ARM, 2GB) + 스왑 2GB, Ubuntu 24.04, 주소 `galgot.duckdns.org`.

---

## 1. 서버 준비 (처음 한 번)

1. EC2 인스턴스(Ubuntu 24.04, ARM), 고정 IP(Elastic IP), 보안 그룹 22(내 IP만)·80·443.
   키 페어 `.pem`은 만들 때 한 번만 내려받을 수 있다. Chrome이 "위험할 수 있음"으로 막으면 다운로드 목록(Ctrl+J)에서 "유지"를 누른다.
   새 AWS 가입 방식의 무료 플랜 프로젝트는 시드니 리전에 묶여 있어 서울에는 만들 수 없었다.
2. DuckDNS에서 주소를 만들고 고정 IP를 넣는다.
3. PC(PowerShell)에서 키 권한을 맞추고 접속한다.

```powershell
icacls "$env:USERPROFILE\.ssh\galgot-key.pem" /inheritance:r /grant:r "$($env:USERNAME):R"
ssh -i "$env:USERPROFILE\.ssh\galgot-key.pem" ubuntu@<고정 IP>
```

4. 서버에서 스왑, 시간대, Docker를 설치한다.

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo timedatectl set-timezone Asia/Seoul
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit   # 다시 접속해야 docker 권한이 적용된다
```

## 2. 코드 받기와 설정

```bash
git clone https://github.com/jinnn0801/galgot.git
cd galgot
cp .env.example .env
chmod 600 .env
sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$(openssl rand -hex 16)/; s/^DB_ROOT_PASSWORD=.*/DB_ROOT_PASSWORD=$(openssl rand -hex 16)/" .env
```

키는 한 개씩 넣는다. 여러 줄을 한꺼번에 붙여넣으면 입력 칸에 다음 줄이 들어가 빈 값이 된다.

```bash
setkey() { read -rp "$1 값을 붙여넣고 Enter: " v && { grep -v "^$1=" .env; printf '%s=%s\n' "$1" "$v"; } > .env.new && mv .env.new .env && echo "$1 저장됨"; }
setkey KAKAO_REST_API_KEY      # 한 줄씩 실행
setkey KAKAO_CLIENT_SECRET
setkey VITE_KAKAO_MAP_KEY      # 카카오 지도 JavaScript 키
setkey TOURAPI_SERVICE_KEY
setkey KMA_SERVICE_KEY
grep -E '=$' .env | cut -d= -f1   # 아무것도 안 나오면 정상
```

`.env`는 git에 올리지 않는다. 키 값은 문서나 대화에 적지 않는다.

## 3. 실행

```bash
docker compose build app    # 메모리가 작아 하나씩 빌드한다
docker compose build web
docker compose up -d
docker compose logs -f app  # "Started GalgotApplication" 이 보이면 Ctrl+C
```

처음 실행하면 Flyway(V1~V8)가 빈 표와 지역·분류 규칙을 만든다. Caddy가 인증서를 받는 데 1분쯤 걸릴 수 있다.

## 4. 장소 데이터 옮기기 (처음 한 번)

PC의 MySQL에서 분류 규칙, 장소, 장소 상세만 내보낸다. 회원·코스·날씨는 옮기지 않는다.
PowerShell의 `>` 는 파일을 UTF-16으로 쓰므로 반드시 `--result-file`을 쓴다.

```powershell
& "C:\Program Files\MySQL\MySQL Server 26.10\bin\mysqldump.exe" -u root -p --no-tablespaces --no-create-info --skip-triggers --replace --set-gtid-purged=OFF --default-character-set=utf8mb4 --result-file=galgot-places.sql galgot env_rules places place_details
scp -i "$env:USERPROFILE\.ssh\galgot-key.pem" galgot-places.sql ubuntu@<고정 IP>:~/
```

- PC의 MySQL 26.10 mysqldump는 마스킹 정책과 테이블스페이스도 읽으려 해서 galgot 계정으로는 권한 오류가 난다. root 계정과 `--no-tablespaces`로 실행한다.
- `mysqldump.exe` 위치는 `Get-ChildItem "C:\Program Files\MySQL" -Recurse -Filter mysqldump.exe`로 찾는다.

서버에서 넣고 개수를 확인한다.

```bash
cd ~/galgot
docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" galgot' < ~/galgot-places.sql
docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" galgot -e "SELECT env, COUNT(*) FROM places GROUP BY env; SELECT COUNT(*) FROM place_details;"'
rm ~/galgot-places.sql
```

PC에서 같은 쿼리를 돌려 개수가 같은지 본다.

## 5. 카카오 개발자 콘솔

앱 "갈곳"에 배포 주소를 추가한다(localhost는 개발용으로 남긴다).

- 플랫폼 > Web > 사이트 도메인: `https://galgot.duckdns.org`
- JavaScript SDK 도메인(지도): `https://galgot.duckdns.org`
- 카카오 로그인 > Redirect URI: `https://galgot.duckdns.org/login/oauth2/code/kakao`

## 6. 새 버전 올리기

PC에서 main에 합치고 푸시한 뒤, 서버에서:

```bash
cd ~/galgot
git pull
docker compose build app web
docker compose up -d
```

앱을 다시 켜면 세션이 메모리에 있어 로그인이 풀린다(알려진 한계).

## 7. 백업과 점검

```bash
# DB 백업(서버 홈 폴더에 파일로)
docker compose exec -T db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction galgot' > ~/galgot-backup-$(date +%F).sql

# 상태와 로그
docker compose ps
docker compose logs --tail=100 app
free -h ; df -h /
```

## 8. 비용 주의 (AWS 무료 플랜)

- 무료 플랜은 크레딧(100달러)과 6개월 안에서만 쓴다. 넘으면 계정이 닫히고 요금은 청구되지 않는다.
- 서버는 1대만. 쓰지 않는 Elastic IP는 바로 반납한다. RDS, 로드밸런서, NAT 게이트웨이는 만들지 않는다.
- 집 IP가 바뀌어 SSH가 막히면 보안 그룹의 SSH 규칙을 "내 IP"로 다시 고른다.
