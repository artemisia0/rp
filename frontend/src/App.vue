<script setup>
import { useCounterStore } from '@/stores/counter';

const counter = useCounterStore();
</script>

<template>
  <button @click="counter.increment">
    Count: {{ counter.count }} (Double: {{ counter.doubleCount }})
  </button>
</template>
