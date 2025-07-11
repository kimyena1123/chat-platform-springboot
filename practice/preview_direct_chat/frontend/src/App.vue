<template>
  <div id="app">
    <div v-if="username">
      <ChatBox :username="username" />
    </div>
    <div v-else class="username-form">
      <h2>사용자 이름을 입력하세요</h2>
      <input v-model="tempName" @keydown.enter="submitName" />
      <button @click="submitName">입장</button>
    </div>
  </div>
</template>

<script>
import ChatBox from './components/ChatBox.vue'

export default {
  name: 'App',
  components: {
    ChatBox
  },
  data() {
    return {
      tempName: '',
      username: ''
    }
  },
  methods: {
    submitName() {
      if (this.tempName.trim() !== '') {
        this.username = this.tempName.trim();
      }
    }
  }
}
</script>

<style>
#app {
  font-family: Arial, sans-serif;
  text-align: center;
  margin-top: 40px;
}

.username-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 300px;
  margin: auto;
}

input {
  padding: 8px;
  font-size: 1rem;
  border-radius: 4px;
  border: 1px solid #ccc;
}

button {
  padding: 8px 12px;
  font-size: 1rem;
  border-radius: 4px;
  border: none;
  background-color: #2196f3;
  color: white;
  cursor: pointer;
}

button:hover {
  background-color: #1976d2;
}
</style>
