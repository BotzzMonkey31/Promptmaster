<template>
  <div>
    <component
      :is="currentComponent"
      v-if="currentComponent && puzzle"
      :puzzle="puzzle"
    />
    <div v-else-if="loading">Loading...</div>
    <div v-else-if="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import apiClient from '../services/api';
import MultiStepSolve from './MultiStepSolve.vue';
// import FaultySolve from './FaultySolve.vue';
// import BypassSolve from './BypassSolve.vue';

const route = useRoute();
const puzzle = ref<any>(null);
const loading = ref(true);
const error = ref('');

const fetchPuzzle = async () => {
  loading.value = true;
  error.value = '';
  try {
    const id = route.params.id;
    const response = await apiClient.get(`/puzzles/${id}`);
    puzzle.value = response.data;
  } catch (e) {
    error.value = 'Puzzle not found.';
  } finally {
    loading.value = false;
  }
};

onMounted(fetchPuzzle);

const currentComponent = computed(() => {
  if (!puzzle.value) return null;
  switch (puzzle.value.type) {
    case 'Multi_Step':
      return MultiStepSolve;
    // case 'Faulty':
    //   return FaultySolve;
    // case 'BY_PASS':
    //   return BypassSolve;
    default:
      return null;
  }
});
</script>
