<script setup>
import { ref, onMounted } from 'vue';

const snapshots = ref([]);
const diffs = ref({});
const loading = ref(true);
const error = ref(null);

onMounted(async () => {
  try {
    const snapRes = await fetch('/api/snapshots');
    if (!snapRes.ok) throw new Error('Failed to load snapshots');
    snapshots.value = await snapRes.json();

    const diffsRes = await fetch('/api/diffs');
    if (!diffsRes.ok) throw new Error('Failed to load diffs');
    const allDiffs = await diffsRes.json();
    diffs.value = Object.fromEntries(allDiffs.map(d => [d.snapshotId, d]));
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <p v-if="loading">Loading…</p>
    <p v-else-if="error">Error: {{ error }}</p>

    <ul v-else>
      <li v-for="s in snapshots" :key="s.snapshotId">
        <strong>Snapshot {{ s.snapshotId }}</strong>
        ({{ s.operation }})

        <div v-if="diffs[s.snapshotId]">
          <p>Added files:</p>
          <ul>
            <li v-for="f in diffs[s.snapshotId].addedFiles" :key="f">
              {{ f }}
            </li>
          </ul>

          <p>Removed files:</p>
          <ul>
            <li v-for="f in diffs[s.snapshotId].removedFiles" :key="f">
              {{ f }}
            </li>
          </ul>
        </div>
      </li>
    </ul>
  </div>
</template>
