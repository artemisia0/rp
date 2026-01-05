<script setup>
import { ref, onMounted } from 'vue';

const snapshots = ref([]);
const diffs = ref({});

onMounted(async () => {
  snapshots.value = await fetch('/api/snapshots')
    .then(r => r.json());

  for (const s of snapshots.value) {
    diffs.value[s.snapshotId] =
      await fetch(`/api/diffs/${s.snapshotId}`)
        .then(r => r.json());
  }
});
</script>

<template>
  <ul>
    <li v-for="s in snapshots" :key="s.snapshotId">
      Snapshot {{ s.snapshotId }}
      <ul>
        <li>Added files: {{ diffs[s.snapshotId]?.addedFiles }}</li>
        <li>Removed files: {{ diffs[s.snapshotId]?.removedFiles }}</li>
      </ul>
    </li>
  </ul>
</template>
