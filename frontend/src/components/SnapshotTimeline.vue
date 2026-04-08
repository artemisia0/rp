<script setup>
import { computed, ref } from 'vue';
import { formatTimestamp } from '../utils/dataUtils';

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

const viewMode = ref('stacked');

const timelinePoints = computed(() =>
  props.snapshots.map((snapshot) => {
    const diff = props.diffsBySnapshot[snapshot.snapshotId];
    const changes = diff?.rowChanges ?? [];
    const countByType = changes.reduce(
      (acc, change) => {
        acc[change.type] = (acc[change.type] || 0) + 1;
        return acc;
      },
      { ADDED: 0, UPDATED: 0, REMOVED: 0 },
    );

    return {
      snapshotId: snapshot.snapshotId,
      label: formatTimestamp(snapshot.timestampMillis) || `Snapshot ${snapshot.snapshotId}`,
      added: countByType.ADDED,
      updated: countByType.UPDATED,
      removed: countByType.REMOVED,
      total: countByType.ADDED + countByType.UPDATED + countByType.REMOVED,
    };
  }),
);

const totals = computed(() =>
  timelinePoints.value.reduce(
    (acc, point) => {
      acc.added += point.added;
      acc.updated += point.updated;
      acc.removed += point.removed;
      acc.total += point.total;
      return acc;
    },
    { added: 0, updated: 0, removed: 0, total: 0 },
  ),
);

const busiestSnapshot = computed(() => {
  if (timelinePoints.value.length === 0) return null;
  return timelinePoints.value.reduce((best, current) =>
    current.total > best.total ? current : best,
  );
});

const chartOption = computed(() => ({
  title: {
    text: 'Snapshot Timeline',
    left: 'left',
    textStyle: { fontSize: 16 },
  },
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data:
      viewMode.value === 'stacked'
        ? ['Added', 'Updated', 'Removed']
        : ['Total changes'],
    top: 28,
  },
  grid: {
    left: 50,
    right: 20,
    bottom: 50,
    top: 70,
  },
  xAxis: {
    type: 'category',
    data: timelinePoints.value.map((point) => point.label),
    axisLabel: {
      show: false,
    },
    axisTick: {
      show: false,
    },
  },
  yAxis: {
    type: 'value',
    name: 'Row changes',
  },
  dataZoom: [
    {
      type: 'slider',
      bottom: 10,
      height: 16,
    },
    {
      type: 'inside',
    },
  ],
  series:
    viewMode.value === 'stacked'
      ? [
          {
            name: 'Added',
            type: 'bar',
            stack: 'changes',
            data: timelinePoints.value.map((point) => point.added),
            itemStyle: { color: '#22c55e' },
          },
          {
            name: 'Updated',
            type: 'bar',
            stack: 'changes',
            data: timelinePoints.value.map((point) => point.updated),
            itemStyle: { color: '#eab308' },
          },
          {
            name: 'Removed',
            type: 'bar',
            stack: 'changes',
            data: timelinePoints.value.map((point) => point.removed),
            itemStyle: { color: '#ef4444' },
          },
        ]
      : [
          {
            name: 'Total changes',
            type: 'line',
            data: timelinePoints.value.map((point) => point.total),
            smooth: true,
            symbolSize: 8,
            itemStyle: { color: '#4f46e5' },
            lineStyle: { width: 3 },
            areaStyle: { color: 'rgba(79, 70, 229, 0.12)' },
            markPoint: {
              data: [{ type: 'max', name: 'Max' }],
            },
          },
        ],
}));
</script>

<template>
  <div class="timeline">
    <div class="summary">
      <div>
        <strong>Total changes</strong>
        <span>{{ totals.total }}</span>
      </div>
      <div>
        <strong>Net change</strong>
        <span>{{ totals.added - totals.removed }}</span>
      </div>
      <div>
        <strong>Busiest snapshot</strong>
        <span v-if="busiestSnapshot">
          {{ busiestSnapshot.label }} ({{ busiestSnapshot.total }})
        </span>
        <span v-else>-</span>
      </div>
    </div>

    <div class="controls">
      <label>
        View
        <select v-model="viewMode">
          <option value="stacked">Stacked bars</option>
          <option value="total">Total line</option>
        </select>
      </label>
    </div>

    <p v-if="timelinePoints.length === 0" class="empty">No snapshots available.</p>
    <VChart v-else class="chart" :option="chartOption" autoresize />
    <div class="legend">
      <span>Added: green</span>
      <span>Updated: yellow</span>
      <span>Removed: red</span>
    </div>
  </div>
</template>

<style scoped>
.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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

.controls {
  display: flex;
  justify-content: flex-end;
}

.controls select {
  margin-left: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.chart {
  width: 100%;
  height: 380px;
}

.empty {
  padding: 24px;
  text-align: center;
  color: #64748b;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 14px;
  color: #475569;
}
</style>
