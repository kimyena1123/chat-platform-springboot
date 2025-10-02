-- [목적]
--  - Replica 서버가 "어떤 Source에", "어떤 계정으로", "어떤 방식으로" 복제를 시작할지 설정한다.
--  - GTID 기반 복제(자동 포지셔닝)를 사용하므로 SOURCE_AUTO_POSITION=1 을 켠다.
--    (→ binlog 파일/좌표를 일일이 수동 지정할 필요 없음)

-- [어디서 실행?]
--  - 반드시 "Replica(복제본) 서버"에서 실행한다. (root 계정 등으로)

-- [사전조건]
--  - Source와 Replica 모두 MySQL 설정에서 GTID 기능이 켜져 있어야 한다:
--      gtid-mode=ON, enforce-gtid-consistency=ON
--  - 소스에 'replica_user' 계정이 존재하고 권한이 부여되어 있어야 한다(1번 스크립트).

-- [Docker Compose 네트워킹 주의]
--  - SOURCE_HOST='mysql-source' 는 docker-compose 의 서비스 이름을 사용한 것.
--    Replica 컨테이너는 같은 네트워크에서 "mysql-source:3306" 으로 접근 가능.
--  - 포트를 커스텀했다면 SOURCE_PORT=... 도 함께 지정 필요.

-- (이미 REPLICA가 돌고 있었다면)
-- STOP REPLICA;  -- 먼저 멈춘 뒤 변경하는 게 안전

CHANGE REPLICATION SOURCE TO
       SOURCE_HOST = 'mysql-source',     -- Source(Primary)의 호스트(Compose 서비스명)
       SOURCE_USER = 'replica_user',     -- 1번 스크립트에서 만든 사용자
       SOURCE_PASSWORD = 'replica_password',
       SOURCE_AUTO_POSITION = 1;         -- GTID 자동 포지셔닝(파일/좌표 수동 지정 불필요)

START REPLICA;                           -- 복제 시작 (IO/SQL 스레드 실행)
