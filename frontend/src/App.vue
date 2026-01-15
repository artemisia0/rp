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

        <div v-if="diffs[s.snapshotId]" style="margin: 1.5em 0; padding: 1em; border: 1px solid #eee; border-radius: 8px; background: #fafbfc;">
          <template v-if="diffs[s.snapshotId].addedFiles && diffs[s.snapshotId].addedFiles.length">
            <p style="margin-bottom: 0.3em; font-weight: bold;">Added files:</p>
            <ul style="margin-top: 0; margin-bottom: 0.7em;">
              <li v-for="f in diffs[s.snapshotId].addedFiles" :key="f">{{ f }}</li>
            </ul>
          </template>
          <template v-if="diffs[s.snapshotId].removedFiles && diffs[s.snapshotId].removedFiles.length">
            <p style="margin-bottom: 0.3em; font-weight: bold;">Removed files:</p>
            <ul style="margin-top: 0; margin-bottom: 0.7em;">
              <li v-for="f in diffs[s.snapshotId].removedFiles" :key="f">{{ f }}</li>
            </ul>
          </template>
          <template v-if="diffs[s.snapshotId].rowChanges && diffs[s.snapshotId].rowChanges.length">
            <p style="margin-bottom: 0.3em; font-weight: bold;">Row changes:</p>
            <ul style="margin-top: 0;">
              <li v-for="c in diffs[s.snapshotId].rowChanges" :key="c.type + JSON.stringify(c.before) + JSON.stringify(c.after)" style="margin-bottom: 0.2em;">
                <span v-if="c.type === 'ADDED'">ADDED: {{ c.after }}</span>
                <span v-else-if="c.type === 'REMOVED'">REMOVED: {{ c.before }}</span>
                <span v-else-if="c.type === 'UPDATED'">UPDATED: {{ c.before }} → {{ c.after }}</span>
              </li>
            </ul>
          </template>
        </div>
      </li>
    </ul>
    </div>
</template>
