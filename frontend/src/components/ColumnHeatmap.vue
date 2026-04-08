<script setup>
import { computed, reactive, ref } from 'vue';
import { formatTimestamp, getFieldDiffs } from '../utils/dataUtils';

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

const activeTypes = reactive({
  ADDED: true,
  UPDATED: true,
  REMOVED: true,
});
const metricMode = ref('count');
const columnLimit = ref(12);
const fieldSearch = ref('');

const snapshotInfo = computed(() =>
  props.snapshots.map((snapshot) => ({
    snapshotId: snapshot.snapshotId,
    label: formatTimestamp(snapshot.timestampMillis) || `Snapshot ${snapshot.snapshotId}`,
  })),
);

const snapshotIndex = computed(
  () => new Map(snapshotInfo.value.map((snap, index) => [snap.snapshotId, index])),
);

const changeEntries = (change) => {
  if (change.type === 'UPDATED') {
    return getFieldDiffs(change).filter((field) => field.changed);
  }
  return getFieldDiffs(change);
};

const countsBySnapshot = computed(() => {
  const perSnapshot = new Map();
  for (const diff of props.diffs) {
    if (!snapshotIndex.value.has(diff.snapshotId)) continue;
    const current =
      perSnapshot.get(diff.snapshotId) || ({ fields: new Map(), total: 0 });
    for (const change of diff.rowChanges ?? []) {
      if (!activeTypes[change.type]) continue;
      const entries = changeEntries(change);
      for (const entry of entries) {
        current.fields.set(entry.field, (current.fields.get(entry.field) || 0) + 1);
        current.total += 1;
      }
    }
    perSnapshot.set(diff.snapshotId, current);
  }
  return perSnapshot;
});

const fieldTotals = computed(() => {
  const totals = new Map();
  for (const snapshot of countsBySnapshot.value.values()) {
    for (const [field, count] of snapshot.fields.entries()) {
      totals.set(field, (totals.get(field) || 0) + count);
    }
  }
  return totals;
});

const filteredTotals = computed(() => {
  const query = fieldSearch.value.trim().toLowerCase();
  const entries = [...fieldTotals.value.entries()];
  if (!query) return entries;
  return entries.filter(([field]) => field.toLowerCase().includes(query));
});

const orderedFields = computed(() => {
  const fields = [...filteredTotals.value];
  fields.sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
  const names = fields.map(([name]) => name);
  if (columnLimit.value === 0) return names;
  return names.slice(0, columnLimit.value);
});

const columnIndex = computed(
  () => new Map(orderedFields.value.map((field, index) => [field, index])),
);

const snapshotTotals = computed(() => {
  const totals = new Map();
  for (const [snapshotId, snapshot] of countsBySnapshot.value.entries()) {
    totals.set(snapshotId, snapshot.total);
  }
  return totals;
});

const heatmapData = computed(() => {
  const data = [];
  for (const [rowIndex, snap] of snapshotInfo.value.entries()) {
    const snapshotCounts = countsBySnapshot.value.get(snap.snapshotId);
    const snapshotTotal = snapshotTotals.value.get(snap.snapshotId) || 0;
    for (const field of orderedFields.value) {
      const colIndex = columnIndex.value.get(field);
      if (colIndex === undefined) continue;
      const count = snapshotCounts?.fields.get(field) || 0;
      const value =
        metricMode.value === 'percent' && snapshotTotal > 0
          ? (count / snapshotTotal) * 100
          : count;
      data.push([colIndex, rowIndex, Number(value.toFixed(2))]);
    }
  }
  return data;
});

const maxValue = computed(() =>
  heatmapData.value.reduce((max, item) => Math.max(max, item[2]), 0),
);

const topColumns = computed(() =>
  [...fieldTotals.value.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, 5),
);

const displayedColumns = computed(() =>
  orderedFields.value.map((field) => [field, fieldTotals.value.get(field) || 0]),
);

const totalChanges = computed(() =>
  [...fieldTotals.value.values()].reduce((sum, value) => sum + value, 0),
);

const totalColumns = computed(() => fieldTotals.value.size);

const chartOption = computed(() => ({
  title: {
    text: 'Column Heatmap',
    left: 'left',
    textStyle: { fontSize: 16 },
  },
  tooltip: {
    position: 'top',
    formatter: (params) => {
      const field = orderedFields.value[params.data[0]];
      const snapshot = snapshotInfo.value[params.data[1]];
      const count =
        countsBySnapshot.value.get(snapshot.snapshotId)?.fields.get(field) || 0;
      const total = snapshotTotals.value.get(snapshot.snapshotId) || 0;
      const percent = total > 0 ? ((count / total) * 100).toFixed(1) : '0.0';
      return `${field}<br/>${snapshot.label}<br/>${count} changes (${percent}%)`;
    },
  },
  grid: {
    top: 90,
    bottom: 70,
    left: 160,
    right: 20,
  },
  xAxis: {
    type: 'category',
    data: orderedFields.value,
    axisLabel: {
      rotate: 28,
      interval: 0,
    },
  },
  yAxis: {
    type: 'category',
    data: snapshotInfo.value.map((snap) => snap.label),
  },
  visualMap: {
    min: 0,
    max: metricMode.value === 'percent' ? 100 : Math.max(1, maxValue.value),
    orient: 'horizontal',
    left: 'center',
    bottom: 10,
    formatter: metricMode.value === 'percent' ? '{value}%' : '{value}',
  },
  series: [
    {
      type: 'heatmap',
      data: heatmapData.value,
      label: {
        show: metricMode.value === 'percent',
        formatter: (params) => (params.data[2] ? `${params.data[2]}%` : ''),
        color: '#0f172a',
        fontSize: 10,
      },
      emphasis: {
        itemStyle: {
          borderColor: '#1e293b',
          borderWidth: 1,
        },
      },
    },
  ],
}));
</script>

<template>
  <div class="heatmap">
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
      <div class="selectors">
        <label>
          Field filter
          <input
            v-model="fieldSearch"
            type="search"
            placeholder="Filter columns"
            spellcheck="false"
          />
        </label>
        <label>
          Metric
          <select v-model="metricMode">
            <option value="count">Count</option>
            <option value="percent">Percent per snapshot</option>
          </select>
        </label>
        <label>
          Columns
          <select v-model.number="columnLimit">
            <option :value="8">Top 8</option>
            <option :value="12">Top 12</option>
            <option :value="20">Top 20</option>
            <option :value="0">All</option>
          </select>
        </label>
      </div>
    </div>

    <div class="summary">
      <div>
        <strong>Total changes</strong>
        <span>{{ totalChanges }}</span>
      </div>
      <div>
        <strong>Columns shown</strong>
        <span>{{ orderedFields.length }} of {{ totalColumns }}</span>
      </div>
      <div>
        <strong>Top columns</strong>
        <ol>
          <li v-for="[field, count] in topColumns" :key="field">
            {{ field }} ({{ count }})
          </li>
        </ol>
      </div>
      <div class="column-list">
        <details>
          <summary>Displayed columns</summary>
          <ul>
            <li v-for="[field, count] in displayedColumns" :key="field">
              {{ field }} ({{ count }})
            </li>
          </ul>
        </details>
      </div>
    </div>

    <p v-if="orderedFields.length === 0" class="empty">
      No column change data available for the selected filters.
    </p>
    <VChart v-else class="chart" :option="chartOption" autoresize />
  </div>
</template>

<style scoped>
.heatmap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.filters,
.selectors {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

label {
  display: flex;
  gap: 6px;
  align-items: center;
}

input[type='search'] {
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

select {
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
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

.summary ol {
  margin: 6px 0 0;
  padding-left: 18px;
  color: #475569;
}

.column-list details {
  margin-top: 6px;
}

.column-list summary {
  cursor: pointer;
  font-weight: 600;
  color: #1e293b;
}

.column-list ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
}

.chart {
  width: 100%;
  height: 440px;
}

.empty {
  padding: 24px;
  text-align: center;
  color: #64748b;
}
</style>
