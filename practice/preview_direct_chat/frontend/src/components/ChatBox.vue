<!--채팅 UI + WebSocket 연결-->

<template>
  <div class="chat-container">
    <!-- 현재 접속한 사용자 이름 표시 -->
    <h2>🧑 {{ username }}의 채팅창</h2>
    <div class="chat-box">
      <!-- 메시지 배열을 반복하여 출력 -->
      <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message"
          :class="{
          system: msg.systemMessage,                          // 시스템 메시지인지 여부에 따라 스타일 지정
          mine: msg.name === username,                        // 내가 보낸 메시지이면 오른쪽에 표시
          other: msg.name !== username && !msg.systemMessage  // 상대방이 보낸 메시지이면 왼쪽에 표시
        }"
      >

        <!-- 시스템 메시지인 경우 (ex. "예나님이 입장하셨습니다.") -->
        <template v-if="msg.systemMessage">
          <em>{{ msg.systemMessage }}</em>
        </template>

        <!-- 일반 채팅 메시지인 경우 (사용자 이름 + 내용) -->
        <template v-else>
          <strong>{{ msg.name }}</strong>: {{ msg.content }}
        </template>
      </div>
    </div>

    <!-- 채팅 입력 및 전송 영역 -->
    <div class="input-area">
      <!-- 메시지 입력창 (Enter 키로 전송도 가능) -->
      <!-- @keydown.enter으로 할 시 메시지 두번 보내는 문제 발생 -->
      <input
          v-model="input"
          @keyup.enter="sendMessage"
          placeholder="메시지 입력"
      />

      <!-- 전송 버튼 클릭 시 메시지 전송 -->
      <button @click="sendMessage">보내기</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatBox',
  props: {
    // 부모 컴포넌트에서 전달받는 사용자 이름
    username: String
  },
  data() {
    return {
      ws: null,     // WebSocket 객체
      input: '',    // 현재 입력 중인 메시지 문자열
      messages: []  // 화면에 출력할 메시지 배열
    }
  },
  mounted() {
    // 컴포넌트가 화면에 마운트되면 WebSocket 연결을 시작
    if (!this.username) return;

    // WebSocket 서버 주소 (포트와 endpoint 포함) + 사용자 이름을 쿼리 파라미터로 전달
    const wsUrl = `ws://localhost:8080/ws/v1/message?name=${encodeURIComponent(this.username)}`;
    this.ws = new WebSocket(wsUrl);

    // WebSocket 연결 성공 시
    this.ws.onopen = () => {
      console.log('웹소켓 연결됨');
    }

    // 서버로부터 메시지를 수신했을 때
    this.ws.onmessage = (event) => {
      try {
        // 메시지가 JSON 형태일 경우 (일반 채팅 메시지)
        const msg = JSON.parse(event.data);
        this.messages.push(msg);
      } catch (e) {
        // JSON 파싱 실패 → 단순 텍스트 메시지(입장 메시지 등)
        this.messages.push({ systemMessage: event.data });
      }

      // 새 메시지 수신 후 스크롤을 가장 아래로 이동
      this.$nextTick(() => {
        const box = this.$el.querySelector('.chat-box');
        box.scrollTop = box.scrollHeight;
      });
    }

    // WebSocket 에러 발생 시
    this.ws.onerror = (error) => {
      console.error('웹소켓 에러:', error);
    }

    // WebSocket 연결 종료 시
    this.ws.onclose = () => {
      console.log('웹소켓 연결 종료');
    }
  },
  methods: {
    // !메시지를 서버로 전송하는 함수!
    sendMessage() {
      // 입력창이 비어있지 않고 WebSocket이 열려 있을 경우만 전송
      if (this.input.trim() !== '' && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          content: this.input,  // 메시지 본문
          name: this.username   // 사용자 이름
        }));
        this.input = '';        // 입력창 초기화
      }
    }
  }
}
</script>

<style scoped>
/* 전체 채팅 UI 스타일 */
.chat-container {
  max-width: 600px;
  margin: 20px auto;
  border: 2px solid #ccc;
  padding: 10px;
  border-radius: 8px;
  font-family: Arial, sans-serif;
}

/* 메시지 목록을 보여주는 박스 */
.chat-box {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #eee;
  padding: 10px;
  margin-bottom: 10px;
  background-color: #fafafa;
}

/* 메시지 하나의 스타일 */
.message {
  margin-bottom: 8px;
  max-width: 70%;
  padding: 6px 10px;
  border-radius: 10px;
  clear: both;
  word-break: break-word;
}

/* 시스템 메시지 (입장/퇴장 알림 등) */
.message.system {
  text-align: center;
  color: gray;
  font-style: italic;
  max-width: 100%;
  background: none;
  border-radius: 0;
  margin: 12px 0;
}

/* 내가 보낸 메시지: 오른쪽 정렬 + 초록 배경 */
.message.mine {
  background-color: #dcf8c6;
  float: right;
  text-align: right;
}

/* 상대방 메시지: 왼쪽 정렬 + 회색 배경 */
.message.other {
  background-color: #f1f0f0;
  float: left;
  text-align: left;
}

.input-area {
  display: flex;
  gap: 10px;
}

input {
  flex: 1;
  padding: 5px;
  font-size: 1rem;
  border-radius: 4px;
  border: 1px solid #ccc;
}

button {
  padding: 5px 10px;
  font-size: 1rem;
  border-radius: 4px;
  border: none;
  background-color: #4caf50;
  color: white;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

button:hover {
  background-color: #45a049;
}
</style>
