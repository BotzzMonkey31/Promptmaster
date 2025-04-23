<template>
  <div class="multistep-container">
    <div class="top-section">
      <img class="avatar" alt="Avatar" />
      <div class="text-bubble">{{ textBubble }}</div>
    </div>

    <div class="middle-section">
      <div class="task-box">{{ taskDescription }}</div>
      <div class="input-box">
        <textarea v-model="userInput" placeholder="Enter your input here"></textarea>
        <button @click="handleSubmit">Submit</button>
      </div>
    </div>

    <div class="code-editor" ref="editor"></div>
  </div>
</template>

<script lang="ts">
import { EditorView, lineNumbers, highlightActiveLineGutter, highlightSpecialChars } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import { javascript } from "@codemirror/lang-javascript";
import { keymap } from "@codemirror/view";
import { defaultKeymap } from "@codemirror/commands";
import { syntaxHighlighting, defaultHighlightStyle } from "@codemirror/language";
import { bracketMatching } from "@codemirror/language";
import { closeBrackets } from "@codemirror/autocomplete";
import { history } from "@codemirror/commands";

export default {
  name: 'Multi',
  data() {
    return {
      textBubble: "That's too much in one go, try to split up the task in different steps.",
      taskDescription: "Create a Java program that reads a file, processes the text, counts word occurrences, sorts by frequency, and outputs the top 10 words.",
      userInput: '',
      code: 'public class WordFrequencyCounter {\n  public static void main(String[] args) {\n    // Todo: Implementation\n  }\n}',
      editor: null as EditorView | null
    };
  },
  mounted() {
    const editor = new EditorView({
      state: EditorState.create({
        doc: this.code,
        extensions: [
          lineNumbers(),
          highlightActiveLineGutter(),
          highlightSpecialChars(),
          history(),
          bracketMatching(),
          closeBrackets(),
          javascript(),
          syntaxHighlighting(defaultHighlightStyle),
          keymap.of(defaultKeymap),
          EditorView.lineWrapping
        ],
      }),
      parent: this.$refs.editor as Element
    });

    this.editor = editor;
  },
  beforeDestroy() {
    if (this.editor) {
      this.editor.destroy();
    }
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
  align-items: stretch;
  padding: 24px;
  background-color: #f9f9fc;
  border-radius: 12px;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
  max-width: 900px;
  margin: 20px auto;
  gap: 24px;
}

.top-section {
  display: flex;
  align-items: flex-start;
  width: 100%;
  gap: 16px;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 2px solid #4a6bf5;
  box-shadow: 0 2px 8px rgba(74, 107, 245, 0.2);
  object-fit: cover;
}

.text-bubble {
  flex: 1;
  padding: 16px;
  border-radius: 12px;
  background-color: #ffffff;
  font-size: 15px;
  color: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  line-height: 1.5;
  position: relative;
}

.text-bubble:before {
  content: '';
  position: absolute;
  left: -10px;
  top: 20px;
  width: 0;
  height: 0;
  border-top: 10px solid transparent;
  border-bottom: 10px solid transparent;
  border-right: 10px solid #ffffff;
}

.middle-section {
  display: flex;
  width: 100%;
  gap: 24px;
}

.task-box {
  flex: 1;
  padding: 16px;
  border-radius: 12px;
  background-color: #ffffff;
  font-size: 15px;
  color: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  line-height: 1.5;
}

.input-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

textarea {
  width: 100%;
  height: 120px;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background-color: #ffffff;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
  resize: none;
  font-family: inherit;
  transition: border-color 0.3s, box-shadow 0.3s;
}

textarea:focus {
  outline: none;
  border-color: #4a6bf5;
  box-shadow: 0 0 0 2px rgba(74, 107, 245, 0.2);
}

button {
  align-self: flex-start;
  padding: 10px 24px;
  background-color: #4a6bf5;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s, transform 0.1s;
  box-shadow: 0 2px 4px rgba(74, 107, 245, 0.2);
}

button:hover {
  background-color: #3a57d7;
}

button:active {
  transform: translateY(1px);
}

/* CodeMirror styling */
.code-editor {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* CodeMirror specific styles */
:deep(.cm-editor) {
  height: 100%;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
}

:deep(.cm-scroller) {
  height: 100%;
}

:deep(.cm-gutters) {
  background-color: #282c34;
  color: #6b727f;
  border-right: 1px solid #3e4451;
  padding: 0 5px 0 3px;
}

:deep(.cm-activeLineGutter) {
  background-color: #2c313c;
  color: #a0a8b7;
}

:deep(.cm-content) {
  background-color: #1f2329;
  color: #abb2bf;
}

:deep(.cm-line) {
  padding: 0 4px;
  line-height: 1.6;
}

:deep(.cm-activeLine) {
  background-color: rgba(54, 59, 69, 0.6);
}

:deep(.cm-selectionBackground) {
  background-color: rgba(83, 127, 231, 0.33) !important;
}

:deep(.cm-cursor) {
  border-left: 1.5px solid #528bff;
}

/* Syntax highlighting */
:deep(.cm-keyword) { color: #c678dd; }
:deep(.cm-operator) { color: #56b6c2; }
:deep(.cm-string) { color: #98c379; }
:deep(.cm-number) { color: #d19a66; }
:deep(.cm-comment) { color: #7f848e; font-style: italic; }
:deep(.cm-function) { color: #61afef; }
:deep(.cm-property) { color: #e06c75; }
:deep(.cm-variable) { color: #abb2bf; }
:deep(.cm-type) { color: #e5c07b; }

/* Scrollbar styling */
:deep(.cm-scroller::-webkit-scrollbar) {
  width: 10px;
  height: 10px;
}

:deep(.cm-scroller::-webkit-scrollbar-track) {
  background: #282c34;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb) {
  background: #4a4d57;
  border-radius: 5px;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb:hover) {
  background: #5a5e6a;
}
</style>
