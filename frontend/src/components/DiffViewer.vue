<script setup>
import { computed, reactive, ref } from 'vue';
import { formatTimestamp, formatValue, getFieldDiffs, getRowId } from '../utils/dataUtils';

const props = defineProps({
  snapshots: {
    type: Array,
    default: () => [],
  },
  diffs: {
    type: Array,
    default: () => [],
  },
});

const search = ref('');
const selectedSnapshot = ref('all');
const activeTypes = reactive({
  ADDED: true,
  UPDATED: true,
  REMOVED: true,
});

const snapshotById = computed(() =>
  Object.fromEntries(props.snapshots.map((snapshot) => [snapshot.snapshotId, snapshot])),
);

const rows = computed(() => {
  const result = [];
  for (const diff of props.diffs) {
    if (
      selectedSnapshot.value !== 'all' &&
      String(diff.snapshotId) !== selectedSnapshot.value
    ) {
      continue;
    }
    const snapshot = snapshotById.value[diff.snapshotId];
    const timestamp = snapshot?.timestampMillis ?? null;
    for (const change of diff.rowChanges ?? []) {
      const rowId = getRowId(change);
      if (!rowId) continue;
      const fields =
        change.type === 'UPDATED'
          ? getFieldDiffs(change).filter((field) => field.changed)
          : getFieldDiffs(change);

      for (const field of fields) {
        result.push({
          key: `${diff.snapshotId}-${rowId}-${field.field}-${change.type}-${result.length}`,
          id: rowId,
          field: field.field,
          before: field.before,
          after: field.after,
          type: change.type,
          snapshotId: diff.snapshotId,
          timestamp,
        });
      }
    }
  }
  return result;
});

const filteredRows = computed(() => {
  const query = search.value.trim().toLowerCase();
  return rows.value.filter((row) => {
    if (!activeTypes[row.type]) return false;
    if (!query) return true;
    return (
      row.id.toLowerCase().includes(query) ||
      row.field.toLowerCase().includes(query)
    );
  });
});
</script>

<template>
  <div class="diff-viewer">
    <div class="controls">
      <div class="filters">
        <label>
          <input v-model="activeTypes.ADDED" type="checkbox" />
          Added
        </label>
        <label>
          <input v-model="activeTypes.UPDATED" type="checkbox" />
          Updated
        </label>
        <label>
          <input v-model="activeTypes.REMOVED" type="checkbox" />
          Removed
        </label>
      </div>
      <label class="snapshot-select">
        Snapshot
        <select v-model="selectedSnapshot">
          <option value="all">All</option>
          <option
            v-for="snapshot in snapshots"
            :key="snapshot.snapshotId"
            :value="String(snapshot.snapshotId)"
          >
            {{ formatTimestamp(snapshot.timestampMillis) || `Snapshot ${snapshot.snapshotId}` }}
          </option>
        </select>
      </label>
      <input
        v-model="search"
        type="search"
        placeholder="Search by ID or field"
        spellcheck="false"
      />
    </div>

    <p class="summary">
      Showing {{ filteredRows.length }} of {{ rows.length }} changes.
    </p>

    <div v-if="rows.length === 0" class="empty">No diff data available.</div>
    <div v-else class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Snapshot</th>
            <th>Time</th>
            <th>ID</th>
            <th>Field</th>
            <th>Before</th>
            <th>After</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in filteredRows"
            :key="row.key"
            :class="['diff-row', row.type.toLowerCase()]"
            :title="row.type"
          >
            <td>{{ row.snapshotId }}</td>
            <td>{{ row.timestamp ? formatTimestamp(row.timestamp) : '-' }}</td>
            <td>{{ row.id }}</td>
            <td>{{ row.field }}</td>
            <td>{{ formatValue(row.before) }}</td>
            <td>{{ formatValue(row.after) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="filteredRows.length === 0" class="empty">
        No rows match the current filters.
      </p>
    </div>
  </div>
</template>

<style scoped>
.diff-viewer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-weight: 600;
  color: #1e293b;
}

.snapshot-select {
  display: flex;
  gap: 6px;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.snapshot-select select {
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.controls input[type='search'] {
  min-width: 240px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.summary {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.table-wrapper {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th,
td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: top;
}

th {
  background: #f8fafc;
  font-weight: 700;
  color: #475569;
}

.diff-row.added {
  background: rgba(34, 197, 94, 0.08);
}

.diff-row.updated {
  background: rgba(234, 179, 8, 0.1);
}

.diff-row.removed {
  background: rgba(239, 68, 68, 0.08);
}

.empty {
  padding: 16px;
  text-align: center;
  color: #64748b;
}
</style>
