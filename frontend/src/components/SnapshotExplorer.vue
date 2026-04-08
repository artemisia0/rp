<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { formatTimestamp, formatValue, getFieldDiffs, getRowId } from '../utils/dataUtils';

const props = defineProps({
  snapshots: {
    type: Array,
    default: () => [],
  },
  diffsBySnapshot: {
    type: Object,
    default: () => ({}),
  },
});

const selectedSnapshotId = ref('');
const search = ref('');
const activeTypes = reactive({
  ADDED: true,
  UPDATED: true,
  REMOVED: true,
});

const snapshotOptions = computed(() =>
  props.snapshots.map((snapshot) => ({
    id: String(snapshot.snapshotId),
    label:
      formatTimestamp(snapshot.timestampMillis) || `Snapshot ${snapshot.snapshotId}`,
  })),
);

const selectedSnapshot = computed(() =>
  props.snapshots.find((snapshot) => String(snapshot.snapshotId) === selectedSnapshotId.value),
);

const selectedDiff = computed(() =>
  selectedSnapshot.value ? props.diffsBySnapshot[selectedSnapshot.value.snapshotId] : null,
);

const summaryCounts = computed(() => {
  const counts = { ADDED: 0, UPDATED: 0, REMOVED: 0 };
  if (!selectedDiff.value) return counts;
  for (const change of selectedDiff.value.rowChanges ?? []) {
    counts[change.type] = (counts[change.type] || 0) + 1;
  }
  return counts;
});

const filteredChanges = computed(() => {
  const query = search.value.trim().toLowerCase();
  if (!selectedDiff.value) return [];
  return (selectedDiff.value.rowChanges ?? [])
    .filter((change) => activeTypes[change.type])
    .map((change, index) => {
      const rowId = getRowId(change) || '-';
      const allFields = getFieldDiffs(change);
      const defaultFields =
        change.type === 'UPDATED'
          ? allFields.filter((field) => field.changed)
          : allFields;
      const matchingFields = query
        ? allFields.filter((field) => {
            const before = formatValue(field.before).toLowerCase();
            const after = formatValue(field.after).toLowerCase();
            return (
              field.field.toLowerCase().includes(query) ||
              before.includes(query) ||
              after.includes(query)
            );
          })
        : [];
      const rowMatches = query ? rowId.toLowerCase().includes(query) : true;
      if (query && !rowMatches && matchingFields.length === 0) return null;
      const fields = query
        ? matchingFields.length > 0
          ? matchingFields
          : defaultFields
        : defaultFields;
      return {
        key: `${selectedSnapshotId.value}-${rowId}-${change.type}-${index}`,
        rowId,
        type: change.type,
        fields,
      };
    })
    .filter(Boolean);
});

watch(
  snapshotOptions,
  (options) => {
    if (!options.length) {
      selectedSnapshotId.value = '';
      return;
    }
    if (!selectedSnapshotId.value) {
      selectedSnapshotId.value = options[options.length - 1].id;
    }
  },
  { immediate: true },
);
</script>

<template>
  <div class="explorer">
    <div class="controls">
      <label>
        Snapshot
        <select v-model="selectedSnapshotId">
          <option v-for="option in snapshotOptions" :key="option.id" :value="option.id">
            {{ option.label }}
          </option>
        </select>
      </label>
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
      <input
        v-model="search"
        type="search"
        placeholder="Search by row ID or field"
        spellcheck="false"
      />
    </div>

    <div v-if="!selectedSnapshot" class="empty">No snapshot selected.</div>
    <div v-else class="details">
      <div class="summary">
        <div>
          <strong>Snapshot</strong>
          <span>{{ selectedSnapshot.snapshotId }}</span>
        </div>
        <div>
          <strong>Timestamp</strong>
          <span>{{ formatTimestamp(selectedSnapshot.timestampMillis) }}</span>
        </div>
        <div>
          <strong>Added</strong>
          <span>{{ summaryCounts.ADDED }}</span>
        </div>
        <div>
          <strong>Updated</strong>
          <span>{{ summaryCounts.UPDATED }}</span>
        </div>
        <div>
          <strong>Removed</strong>
          <span>{{ summaryCounts.REMOVED }}</span>
        </div>
      </div>

      <div class="files">
        <div>
          <strong>Added files</strong>
          <ul>
            <li v-for="file in selectedDiff?.addedFiles || []" :key="file">{{ file }}</li>
            <li v-if="!selectedDiff?.addedFiles?.length" class="muted">None</li>
          </ul>
        </div>
        <div>
          <strong>Removed files</strong>
          <ul>
            <li v-for="file in selectedDiff?.removedFiles || []" :key="file">{{ file }}</li>
            <li v-if="!selectedDiff?.removedFiles?.length" class="muted">None</li>
          </ul>
        </div>
      </div>

      <div class="changes">
        <p class="summary-text">
          Showing {{ filteredChanges.length }} row changes.
        </p>
        <div v-if="filteredChanges.length === 0" class="empty">
          No changes match the current filters.
        </div>
        <div v-else class="change-list">
          <details v-for="change in filteredChanges" :key="change.key" class="change-item">
            <summary>
              <span class="type" :class="change.type.toLowerCase()">{{ change.type }}</span>
              <span class="row">Row {{ change.rowId }}</span>
              <span class="count">{{ change.fields.length }} fields</span>
            </summary>
            <table>
              <thead>
                <tr>
                  <th>Field</th>
                  <th>Before</th>
                  <th>After</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="field in change.fields" :key="field.field">
                  <td>{{ field.field }}</td>
                  <td>{{ formatValue(field.before) }}</td>
                  <td>{{ formatValue(field.after) }}</td>
                </tr>
              </tbody>
            </table>
          </details>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.explorer {
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

.controls select,
.controls input[type='search'] {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-weight: 600;
  color: #1e293b;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
}

.summary strong {
  display: block;
  color: #0f172a;
}

.summary span {
  color: #475569;
}

.files {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.files ul {
  margin: 6px 0 0;
  padding-left: 18px;
  color: #475569;
  font-size: 14px;
}

.muted {
  color: #94a3b8;
}

.changes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-text {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.change-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.change-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px 12px;
  background: #ffffff;
}

summary {
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;
  font-weight: 600;
  color: #0f172a;
}

.type {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  text-transform: uppercase;
}

.type.added {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.type.updated {
  background: rgba(234, 179, 8, 0.12);
  color: #a16207;
}

.type.removed {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.row {
  color: #1e293b;
}

.count {
  color: #64748b;
  font-size: 12px;
}

table {
  width: 100%;
  margin-top: 10px;
  border-collapse: collapse;
  font-size: 13px;
}

th,
td {
  text-align: left;
  padding: 6px 8px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: top;
}

th {
  color: #475569;
  font-weight: 600;
}

.empty {
  padding: 16px;
  text-align: center;
  color: #64748b;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
}
</style>
