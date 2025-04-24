<template>
  <div class="flex flex-col items-stretch p-6 bg-light-50 rounded-xl shadow-lg max-w-900px mx-auto my-5 gap-6">
    <div class="flex items-start w-full gap-4">
      <img class="w-15 h-15 rounded-full border-2 border-blue-500 shadow" alt="Avatar" />
      <div class="flex-1 p-4 rounded-3 bg-white text-15px text-gray-800 shadow relative">
        <div class="absolute left--2.5 top-5 border-y-10px border-r-10px border-l-0 border-y-transparent border-r-white"></div>
        {{ textBubble }}
      </div>
    </div>

    <div class="flex w-full gap-6">
      <div class="flex-1 p-4 rounded-3 bg-white text-15px text-gray-800 shadow">
        <h2 class="font-bold text-lg mb-2">{{ puzzle.name }}</h2>
        <div>{{ puzzle.description }}</div>
      </div>
      <div class="flex-1 flex flex-col gap-3 min-w-0">
        <textarea
          v-model="userInput"
          placeholder="Enter your input here"
          class="w-full min-h-24 max-h-40 p-4 border border-gray-200 rounded-2 text-14px text-gray-800 bg-white shadow-sm focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-colors resize-y font-inherit box-border overflow-auto"
        ></textarea>
        <button
          @click="handleSubmit"
          class="self-start px-6 py-2.5 bg-blue-500 text-white border-none rounded-2 cursor-pointer text-14px font-500 transition-colors shadow hover:bg-blue-600 active:translate-y-0.25"
        >Submit</button>
      </div>
    </div>

    <div class="w-full h-75 rounded-2 overflow-hidden shadow" ref="editorDiv"></div>
  </div>
</template>

<script lang="ts" setup>
import { EditorView, lineNumbers, highlightActiveLineGutter, highlightSpecialChars } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import { javascript } from "@codemirror/lang-javascript";
import { keymap } from "@codemirror/view";
import { defaultKeymap } from "@codemirror/commands";
import { syntaxHighlighting, defaultHighlightStyle } from "@codemirror/language";
import { bracketMatching } from "@codemirror/language";
import { closeBrackets } from "@codemirror/autocomplete";
import { history } from "@codemirror/commands";
import { ref, onMounted, watch } from 'vue';
import apiClient from '../services/api'

const props = defineProps<{ puzzle: any }>();
const userInput = ref('');
const code = ref(props.puzzle.code || '');
const editor = ref<EditorView | null>(null);
const editorDiv = ref(null);
const textBubble = ref("That's too much in one go, try to split up the task in different steps.");

onMounted(() => {
  if (editorDiv.value) {
    editor.value = new EditorView({
      state: EditorState.create({
        doc: code.value,
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
          EditorView.lineWrapping,
          EditorView.updateListener.of((v) => {
            if (v.docChanged) {
              code.value = v.state.doc.toString();
            }
          })
        ],
      }),
      parent: editorDiv.value
    });
  }
});

watch(() => props.puzzle.code, (newCode) => {
  if (editor.value && newCode !== code.value) {
    editor.value.dispatch({
      changes: { from: 0, to: editor.value.state.doc.length, insert: newCode || '' }
    });
    code.value = newCode || '';
  }
});

async function handleSubmit() {
  try {
    // Example API call: adjust endpoint and payload as needed
    const response = await apiClient.post('/ai/solve', {
      puzzleId: props.puzzle.id,
      userInput: userInput.value,
      code: code.value,
    });
    // Assume response.data = { text: string, code: string }
    textBubble.value = response.data.text;
    code.value = response.data.code;
    // Optionally, update the editor directly if needed
    if (editor.value) {
      editor.value.dispatch({
        changes: { from: 0, to: editor.value.state.doc.length, insert: response.data.code || '' }
      });
    }
  } catch (error) {
    textBubble.value = 'An error occurred. Please try again.';
    console.error(error);
  }
}
</script>

<style scoped>
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
