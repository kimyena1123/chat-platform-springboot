CREATE TABLE IF NOT EXISTS message_user(
    user_id BIGINT AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    connection_invite_code VARCHAR(20) NOT NULL, -- 초대코드(중복되면 안된다)
    connection_count INT NOT NULL,
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
    partner_b_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    inviter_user_id BIGINT NOT NULL, -- 초대한 사람(초대 요청한 사람)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY(partner_a_user_id, partner_b_user_id), -- primary에 두 개 넣어주면 복합키가 된다
    INDEX idx_partner_b_user_id (partner_b_user_id),
    INDEX idx_partner_a_user_id_status (partner_a_user_id, status),
    INDEX idx_partner_b_usr_id_status (partner_b_user_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;