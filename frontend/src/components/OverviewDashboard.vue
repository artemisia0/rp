<script setup>
import { computed } from 'vue';
import { formatTimestamp, getFieldDiffs, getRowId } from '../utils/dataUtils';

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

const snapshotLabels = computed(() =>
  props.snapshots.map(
    (snapshot) =>
      formatTimestamp(snapshot.timestampMillis) || `Snapshot ${snapshot.snapshotId}`,
  ),
);

const totalsByType = computed(() => {
  const totals = { ADDED: 0, UPDATED: 0, REMOVED: 0 };
  for (const diff of props.diffs) {
    for (const change of diff.rowChanges ?? []) {
      totals[change.type] = (totals[change.type] || 0) + 1;
    }
  }
  return totals;
});

const totalChanges = computed(
  () => totalsByType.value.ADDED + totalsByType.value.UPDATED + totalsByType.value.REMOVED,
);

const netChange = computed(
  () => totalsByType.value.ADDED - totalsByType.value.REMOVED,
);

const changesPerSnapshot = computed(() => {
  const snapshotIds = new Map(
    props.snapshots.map((snapshot, index) => [snapshot.snapshotId, index]),
  );
  const totals = Array(props.snapshots.length).fill(0);
  for (const diff of props.diffs) {
    const index = snapshotIds.get(diff.snapshotId);
    if (index === undefined) continue;
    totals[index] += diff.rowChanges?.length || 0;
  }
  return totals;
});

const busiestSnapshot = computed(() => {
  if (!props.snapshots.length) return null;
  const totals = changesPerSnapshot.value;
  let maxIndex = 0;
  totals.forEach((value, index) => {
    if (value > totals[maxIndex]) maxIndex = index;
  });
  return {
    snapshot: props.snapshots[maxIndex],
    total: totals[maxIndex],
  };
});

const averagePerSnapshot = computed(() => {
  if (!props.snapshots.length) return 0;
  return Math.round((totalChanges.value / props.snapshots.length) * 10) / 10;
});

const topColumns = computed(() => {
  const counts = new Map();
  for (const diff of props.diffs) {
    for (const change of diff.rowChanges ?? []) {
      const fields =
        change.type === 'UPDATED'
          ? getFieldDiffs(change).filter((field) => field.changed)
          : getFieldDiffs(change);
      for (const field of fields) {
        counts.set(field.field, (counts.get(field.field) || 0) + 1);
      }
    }
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, 6);
});

const topRows = computed(() => {
  const counts = new Map();
  for (const diff of props.diffs) {
    for (const change of diff.rowChanges ?? []) {
      const rowId = getRowId(change);
      if (!rowId) continue;
      counts.set(rowId, (counts.get(rowId) || 0) + 1);
    }
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, 6);
});

const typePieOption = computed(() => ({
  title: {
    text: 'Change Type Distribution',
    left: 'left',
    textStyle: { fontSize: 14 },
  },
  tooltip: {
    trigger: 'item',
  },
  legend: {
    bottom: 0,
  },
  series: [
    {
      name: 'Changes',
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: totalsByType.value.ADDED, name: 'Added', itemStyle: { color: '#22c55e' } },
        { value: totalsByType.value.UPDATED, name: 'Updated', itemStyle: { color: '#eab308' } },
        { value: totalsByType.value.REMOVED, name: 'Removed', itemStyle: { color: '#ef4444' } },
      ],
    },
  ],
}));

const trendOption = computed(() => ({
  title: {
    text: 'Changes per Snapshot',
    left: 'left',
    textStyle: { fontSize: 14 },
  },
  tooltip: {
    trigger: 'axis',
  },
  grid: {
    left: 40,
    right: 20,
    top: 50,
    bottom: 40,
  },
  xAxis: {
    type: 'category',
    data: snapshotLabels.value,
    axisLabel: {
      show: false,
    },
    axisTick: {
      show: false,
    },
  },
  yAxis: {
    type: 'value',
  },
  dataZoom: [
    { type: 'slider', bottom: 0, height: 16 },
    { type: 'inside' },
  ],
  series: [
    {
      type: 'bar',
      data: changesPerSnapshot.value,
      itemStyle: { color: '#4f46e5' },
    },
  ],
}));
</script>

<template>
  <div class="overview">
    <div class="kpis">
      <div class="card">
        <strong>Total snapshots</strong>
        <span>{{ snapshots.length }}</span>
      </div>
      <div class="card">
        <strong>Total changes</strong>
        <span>{{ totalChanges }}</span>
      </div>
      <div class="card">
        <strong>Net row change</strong>
        <span>{{ netChange }}</span>
      </div>
      <div class="card">
        <strong>Avg changes / snapshot</strong>
        <span>{{ averagePerSnapshot }}</span>
      </div>
      <div class="card">
        <strong>Busiest snapshot</strong>
        <span v-if="busiestSnapshot">
          {{ formatTimestamp(busiestSnapshot.snapshot.timestampMillis) }} (
          {{ busiestSnapshot.total }})
        </span>
        <span v-else>-</span>
      </div>
    </div>

    <div class="charts">
      <div class="panel">
        <VChart class="chart" :option="typePieOption" autoresize />
      </div>
      <div class="panel">
        <VChart class="chart" :option="trendOption" autoresize />
      </div>
    </div>

    <div class="lists">
      <div class="panel">
        <h3>Top changed columns</h3>
        <ol>
          <li v-for="[field, count] in topColumns" :key="field">
            {{ field }} ({{ count }})
          </li>
        </ol>
        <p v-if="topColumns.length === 0" class="empty">No column changes.</p>
      </div>
      <div class="panel">
        <h3>Most active rows</h3>
        <ol>
          <li v-for="[rowId, count] in topRows" :key="rowId">
            {{ rowId }} ({{ count }})
          </li>
        </ol>
        <p v-if="topRows.length === 0" class="empty">No row changes.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
}

.card strong {
  display: block;
  color: #0f172a;
}

.card span {
  color: #475569;
  font-weight: 600;
}

.charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
}

.chart {
  width: 100%;
  height: 280px;
}

.lists {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.panel h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #0f172a;
}

ol {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  font-size: 14px;
}

.empty {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 13px;
}
</style>
