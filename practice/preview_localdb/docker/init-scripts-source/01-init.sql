-- [목적]
--  - Replica 인스턴스(복제본)가 Source(Primary)에 접속해서 binlog를 읽어갈 수 있도록
--    "복제 전용 사용자"를 Source DB에 만든다.
--  - 이 계정은 "REPLICATION SLAVE/REPLICA" 권한만 가지고, 애플리케이션 쿼리를 실행하지 않는다.

-- [어디서 실행?]
--  - 반드시 "소스(Primary) 서버"에서 실행한다. (root 계정 등으로)

-- [주의]
--  - 'replica_user'@'%' 에서 '%'는 "어떤 호스트에서든 접속 허용"을 의미한다.
--    실제 운영에서는 보안상 특정 IP/대역으로 제한하는 게 안전하다. 예) 'replica_user'@'10.0.%'

-- [비밀번호 플러그인에 대한 설명]
--  - MySQL 8의 기본 인증 플러그인은 `caching_sha2_password` 이다.
--  - 여기서는 간편한 테스트/호환을 위해 `mysql_native_password`를 명시적으로 사용.
--    (운영환경에서는 기본값 사용 또는 TLS를 포함한 더 안전한 구성을 권장)

CREATE USER 'replica_user'@'%'
  IDENTIFIED WITH 'mysql_native_password' BY 'replica_password';

-- [권한 설명]
--  - "REPLICATION SLAVE" 권한은 Replica가 Source의 binary log를 읽을 수 있게 해주는 권한.
--  - MySQL 8.0.23+에서는 용어 변경에 따라 "REPLICATION REPLICA" 가 동의어로 존재하지만,
--    "REPLICATION SLAVE"도 여전히 동작(호환성).
GRANT REPLICATION SLAVE ON *.* TO 'replica_user'@'%';

-- [FLUSH PRIVILEGES]
--  - MySQL 8에서는 CREATE USER/GRANT가 즉시 적용되므로 보통 불필요하지만,
--    초기 스크립트에서는 안전하게 명시해도 무방.
FLUSH PRIVILEGES;
