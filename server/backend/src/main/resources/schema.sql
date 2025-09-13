CREATE TABLE IF NOT EXISTS message_user(
    user_id BIGINT AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    connection_invite_code VARCHAR(100) NOT NULL, -- 초대코드(중복되면 안된다): 사용자마다 고유한 초대 코드. 다른 사용자가 이 코드를 주면 초대 대상(코드 주인)을 찾을 수 있음.
    connection_count INT NOT NULL, -- 친구/연결 수 카운트(한명당 1000명까지의 연결만 가능하다. 그 이상은 X)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
PRIMARY KEY(user_id),
CONSTRAINT unique_username UNIQUE (username), --제약 조건
CONSTRAINT unique_connection_invite_code UNIQUE (connection_invite_code) --제약 조건
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS message(
    message_sequence BIGINT AUTO_INCREMENT,
    user_name VARCHAR(20) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
PRIMARY KEY(message_sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_connection(
    partner_a_user_id BIGINT NOT NULL, --AUTO_INCREMENT 안쓴다. 복합키로 사용(서버가 키를 만들어서 insert)
    partner_b_user_id BIGINT NOT NULL, --두 사용자 간의 관계을 하나의 행(row)에 저장
    status VARCHAR(20) NOT NULL, --관계 상태 (NONE / PENDING / ACCEPTED / REJECTED / DISCONNECTED 등)
    inviter_user_id BIGINT NOT NULL, -- 누가 초대(요청)를 보냈는지(초대자 ID): 초대한 사람(초대 요청한 사람)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY(partner_a_user_id, partner_b_user_id), -- primary에 두 개 넣어주면 복합키가 된다
    INDEX idx_partner_b_user_id (partner_b_user_id),
    INDEX idx_partner_a_user_id_status (partner_a_user_id, status),
    INDEX idx_partner_b_usr_id_status (partner_b_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 복합키?
-- 한 테이블에서 PK(Primary Key)를 여러 컬럼을 묶어서 만드는 것
-- 즉, 한 컬럼만으로는 행(row)을 유일하게 식별할 수 없을 때 여러 컬럼을 합쳐서 유일성을 보장하는 방법


-- ##############################################################################################################

-- 실제 흐름 예시 (다이렉트 채널 생성 & 입장)
-- 1. A와 B가 처음 다이렉트 채팅을 시작 → channel에 새로운 row 생성 (title=“A-B 다이렉트 채널”, invite_code=랜덤)
-- 2. A, B 각각 channel_user에 row 생성 (user_id=A, channel_id=X / user_id=B, channel_id=X)
-- 3. 이후 메시지를 주고받으면 last_read_msg_seq가 업데이트 되면서 읽음/안 읽음 관리

-- channel: 채널 자체의 정적 정보 (이름, 초대코드, 인원수, 생성일 등)
CREATE TABLE IF NOT EXISTS channel(
    channel_id BIGINT AUTO_INCREMENT,
    title VARCHAR(30) NOT NULL, -- 체널 이름
    channel_invite_code VARCHAR(32) NOT NULL, -- 채널 초대 코드(중복X)
    head_count INT NOT NULL, -- 현재 채널에 참여 중인 인원 수
    created_at TIMESTAMP NOT NULL, -- 채널이 생성된 시각. 채널 목록 정렬 시 주로 사용.
    updated_at TIMESTAMP NOT NULL,  -- 채널의 메타데이터(예: 제목 변경)가 마지막으로 수정된 시각.
    PRIMARY KEY (channel_id),
    CONSTRAINT unique_channel_invite_code UNIQUE (channel_invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- channel_user: 채널과 사용자 간의 동적 관계 (참여 여부, 읽은 메시지 위치, 입장 시각 등)
CREATE TABLE IF NOT EXISTS channel_user(
    user_id BIGINT NOT NULL,    -- 복합키; 사용자 식별자(유저 테이블의 PK와 매핑)
    channel_id BIGINT NOT NULL, -- 복합키; 채널 식별자(channel 테이블의 PK와 매핑)
    last_read_msg_seq BIGINT NOT NULL, -- 해당 사용자가 채널에서 마지막으로 읽은 메시지의 시퀀스 번호. → 안 읽은 메시지 개수 계산 시 사용
    created_at TIMESTAMP NOT NULL,  -- 사용자가 채널에 처음 입장한 시각.
    updated_at TIMESTAMP NOT NULL,  -- 마지막으로 갱신된 시각
    PRIMARY KEY (user_id, channel_id),
    INDEX idx_channel_id (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- channel_user에서의 복합키는?
-- channel_user 테이블에서는 user_id + channel_id를 합쳐야 한 행이 유일함을 보장
-- 즉, 같은 사용자가 같은 채널에 중복으로 들어갈 수 없게 하는 것