<script setup>
import { computed, reactive, ref } from 'vue';
import {
  formatTimestamp,
  formatValue,
  getFieldDiffs,
  getRowId,
} from '../utils/dataUtils';

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

const rowIdInput = ref('');
const fieldSearch = ref('');
const showUnchanged = ref(false);
const activeTypes = reactive({
  ADDED: true,
  UPDATED: true,
  REMOVED: true,
});

const normalizedRowId = computed(() => rowIdInput.value.trim());

const snapshotMeta = computed(() =>
  Object.fromEntries(
    props.snapshots.map((snapshot, index) => [
      snapshot.snapshotId,
      { timestampMillis: snapshot.timestampMillis, index },
    ]),
  ),
);

const rowCounts = computed(() => {
  const counts = new Map();
  for (const diff of props.diffs) {
    for (const change of diff.rowChanges ?? []) {
      const rowId = getRowId(change);
      if (!rowId) continue;
      counts.set(rowId, (counts.get(rowId) || 0) + 1);
    }
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
});

const quickPicks = computed(() => rowCounts.value.slice(0, 10));

const events = computed(() => {
  const rowId = normalizedRowId.value;
  if (!rowId) return [];

  const query = fieldSearch.value.trim().toLowerCase();
  const results = [];
  for (const diff of props.diffs) {
    const meta = snapshotMeta.value[diff.snapshotId];
    const timestampMillis = meta?.timestampMillis ?? null;
    const orderIndex = meta?.index ?? Number.MAX_SAFE_INTEGER;
    for (const change of diff.rowChanges ?? []) {
      if (getRowId(change) !== rowId) continue;
      if (!activeTypes[change.type]) continue;

      const allFields = getFieldDiffs(change);
      const changedFields = allFields.filter((field) => field.changed);
      let displayFields;
      if (query) {
        displayFields = allFields.filter((field) => {
          const before = formatValue(field.before).toLowerCase();
          const after = formatValue(field.after).toLowerCase();
          return (
            field.field.toLowerCase().includes(query) ||
            before.includes(query) ||
            after.includes(query)
          );
        });
      } else {
        displayFields = showUnchanged.value ? allFields : changedFields;
      }

      if (displayFields.length === 0) continue;

      results.push({
        key: `${diff.snapshotId}-${change.type}-${results.length}`,
        snapshotId: diff.snapshotId,
        timestampMillis,
        orderIndex,
        type: change.type,
        before: change.before,
        after: change.after,
        fieldDiffs: displayFields,
        shownCount: displayFields.length,
        changedCount: changedFields.length,
        totalFields: allFields.length,
      });
    }
  }

  return results.sort((a, b) => a.orderIndex - b.orderIndex);
});

const summary = computed(() => {
  if (!events.value.length) return null;
  const first = events.value[0];
  const last = events.value[events.value.length - 1];
  const status = last.type === 'REMOVED' ? 'Removed' : 'Active';
  const currentState = last.type === 'REMOVED' ? null : last.after;
  return {
    firstTimestamp: first.timestampMillis,
    lastTimestamp: last.timestampMillis,
    status,
    eventCount: events.value.length,
    currentState,
  };
});

const setRowId = (rowId) => {
  rowIdInput.value = rowId;
};
</script>

<template>
  <div class="row-history">
    <div class="controls">
      <label>
        Row ID
        <input
          v-model="rowIdInput"
          type="text"
          placeholder="Enter row ID"
          spellcheck="false"
          list="row-ids"
        />
      </label>
      <datalist id="row-ids">
        <option v-for="[rowId] in quickPicks" :key="rowId" :value="rowId" />
      </datalist>
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
        <label>
          <input v-model="showUnchanged" type="checkbox" />
          Show unchanged fields
        </label>
      </div>
      <label>
        Field filter
        <input
          v-model="fieldSearch"
          type="search"
          placeholder="Search field name or value"
          spellcheck="false"
        />
      </label>
      <p class="hint">
        Each event shows the row state before/after the change. Updated rows highlight
        only the fields that changed unless you enable unchanged fields.
      </p>
    </div>

    <div v-if="quickPicks.length" class="quick-picks">
      <strong>Quick picks</strong>
      <div class="chips">
        <button
          v-for="[rowId, count] in quickPicks"
          :key="rowId"
          type="button"
          @click="setRowId(rowId)"
        >
          {{ rowId }} ({{ count }})
        </button>
      </div>
    </div>

    <p v-if="!normalizedRowId" class="empty">Enter a row ID to see its history.</p>
    <p v-else-if="events.length === 0" class="empty">No changes found for this row.</p>

    <div v-else class="timeline">
      <div v-if="summary" class="summary">
        <div>
          <strong>Status</strong>
          <span>{{ summary.status }}</span>
        </div>
        <div>
          <strong>Events</strong>
          <span>{{ summary.eventCount }}</span>
        </div>
        <div>
          <strong>First seen</strong>
          <span>{{ formatTimestamp(summary.firstTimestamp) }}</span>
        </div>
        <div>
          <strong>Last change</strong>
          <span>{{ formatTimestamp(summary.lastTimestamp) }}</span>
        </div>
      </div>

      <div v-for="event in events" :key="event.key" class="event">
        <div class="dot" :class="event.type.toLowerCase()" />
        <details class="content">
          <summary class="title">
            <strong>{{ event.type }}</strong>
            <span>Snapshot {{ event.snapshotId }}</span>
            <span v-if="event.timestampMillis">{{
              formatTimestamp(event.timestampMillis)
            }}</span>
            <span class="count">
              Showing {{ event.shownCount }} of {{ event.totalFields }} fields
              ({{ event.changedCount }} changed)
            </span>
          </summary>
          <div v-if="event.fieldDiffs.length === 0" class="empty">
            No fields match the current filters.
          </div>
          <table v-else>
            <thead>
              <tr>
                <th>Field</th>
                <th>Before</th>
                <th>After</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="field in event.fieldDiffs"
                :key="field.field"
                :class="{ changed: field.changed }"
              >
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
</template>

<style scoped>
.row-history {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.controls label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-weight: 600;
  color: #1e293b;
}

.controls input[type='text'],
.controls input[type='search'] {
  max-width: 360px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.hint {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.quick-picks {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
}

.quick-picks strong {
  color: #0f172a;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chips button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  border-radius: 999px;
  padding: 4px 10px;
  cursor: pointer;
  font-size: 13px;
}

.empty {
  padding: 20px;
  text-align: center;
  color: #64748b;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
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

.event {
  display: grid;
  grid-template-columns: 24px 1fr;
  gap: 12px;
  align-items: start;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-top: 6px;
  background: #94a3b8;
}

.dot.added {
  background: #22c55e;
}

.dot.updated {
  background: #eab308;
}

.dot.removed {
  background: #ef4444;
}

.content {
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
}

.title {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 14px;
  color: #0f172a;
  cursor: pointer;
}

.count {
  color: #64748b;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 10px;
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

tr.changed td {
  background: rgba(79, 70, 229, 0.06);
}
</style>
