<template>
  <div class="multistep-container">
    <div class="top-section">
      <img class="avatar" src="../../../../../../Downloads/20241113_190031.jpg" alt="Avatar" />
      <div class="text-bubble">{{ textBubble }}</div>
    </div>

    <div class="middle-section">
      <div class="task-box">{{ taskDescription }}</div>
      <div class="input-box">
        <textarea v-model="userInput" placeholder="Enter your input here"></textarea>
        <button @click="handleSubmit">Submit</button>
      </div>
    </div>

    <div class="code-editor">
      <textarea v-model="code" placeholder="Write your code here"></textarea>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Multistep',
  data() {
    return {
      textBubble: "That's too much in one go, try to split up the task in different steps.",
      taskDescription: "Create a Java program that reads a file, processes the text, counts word occurrences, sorts by frequency, and outputs the top 10 words.",
      userInput: '',
      code: ''
    };
  },
  methods: {
    handleSubmit() {
      console.log('User input:', this.userInput);
    }
  }
};
</script>

<style scoped>
.multistep-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.top-section {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 20px;
}

.avatar {
  width: 100px;
  height: 100px;
}

.text-bubble {
  flex: 1;
  margin-left: 20px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 10px;
  background-color: #f9f9f9;
}

.middle-section {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 20px;
}

.task-box {
  flex: 1;
  margin-right: 20px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 10px;
  background-color: #f9f9f9;
}

.input-box {
  flex: 1;
  display: flex;
  flex-direction: column;
}

textarea {
  width: 100%;
  height: 100px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

button {
  align-self: flex-start;
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}

.code-editor {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #f9f9f9;
}
</style>
