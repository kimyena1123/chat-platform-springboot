<template>
  <div class="chat-container">
    <h2>🧑 {{ username }}의 채팅창</h2>
    <div class="chat-box">
      <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message"
          :class="{
          system: msg.systemMessage,
          mine: msg.name === username,
          other: msg.name !== username && !msg.systemMessage
        }"
      >
        <template v-if="msg.systemMessage">
          <em>{{ msg.systemMessage }}</em>
        </template>
        <template v-else>
          <strong>{{ msg.name }}</strong>: {{ msg.content }}
        </template>
      </div>
    </div>
    <div class="input-area">
      <input
          v-model="input"
          @keyup.enter="sendMessage"
          placeholder="메시지 입력"
      />
      <button @click="sendMessage">보내기</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatBox',
  props: {
    username: String
  },
  data() {
    return {
      ws: null,
      input: '',
      messages: []
    }
  },
  mounted() {
    if (!this.username) return;

    const wsUrl = `ws://localhost:8080/ws/v1/message?name=${encodeURIComponent(this.username)}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('웹소켓 연결됨');
    }

    this.ws.onmessage = (event) => {
      try {
        // JSON 파싱 시도
        const msg = JSON.parse(event.data);
        this.messages.push(msg);
      } catch (e) {
        // JSON 파싱 실패 → 단순 텍스트 메시지(입장 메시지 등)
        this.messages.push({ systemMessage: event.data });
      }
      this.$nextTick(() => {
        const box = this.$el.querySelector('.chat-box');
        box.scrollTop = box.scrollHeight;
      });
    }

    this.ws.onerror = (error) => {
      console.error('웹소켓 에러:', error);
    }

    this.ws.onclose = () => {
      console.log('웹소켓 연결 종료');
    }
  },
  methods: {
    sendMessage() {
      if (this.input.trim() !== '' && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          content: this.input,
          name: this.username
        }));
        this.input = '';
      }
    }
  }
}
</script>

<style scoped>
.chat-container {
  max-width: 600px;
  margin: 20px auto;
  border: 2px solid #ccc;
  padding: 10px;
  border-radius: 8px;
  font-family: Arial, sans-serif;
}

.chat-box {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #eee;
  padding: 10px;
  margin-bottom: 10px;
  background-color: #fafafa;
}

.message {
  margin-bottom: 8px;
  max-width: 70%;
  padding: 6px 10px;
  border-radius: 10px;
  clear: both;
  word-break: break-word;
}

/* 입장 메시지 등 시스템 메시지 */
.message.system {
  text-align: center;
  color: gray;
  font-style: italic;
  max-width: 100%;
  background: none;
  border-radius: 0;
  margin: 12px 0;
}

/* 내가 보낸 메시지: 오른쪽 정렬, 배경색 다름 */
.message.mine {
  background-color: #dcf8c6;
  float: right;
  text-align: right;
}

/* 상대 메시지: 왼쪽 정렬 */
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
