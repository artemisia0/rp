<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import SnapshotTimeline from './components/SnapshotTimeline.vue';
import RowHistory from './components/RowHistory.vue';
import DiffViewer from './components/DiffViewer.vue';
import ColumnHeatmap from './components/ColumnHeatmap.vue';
import OverviewDashboard from './components/OverviewDashboard.vue';
import SnapshotExplorer from './components/SnapshotExplorer.vue';
import SqlWorkbench from './components/SqlWorkbench.vue';
import { formatTimestamp } from './utils/dataUtils';

const snapshots = ref([]);
const diffs = ref([]);
const loading = ref(true);
const error = ref(null);
const activeTab = ref('overview');

const tabs = [
  { id: 'overview', label: 'Overview' },
  { id: 'timeline', label: 'Snapshot Timeline' },
  { id: 'row-history', label: 'Row History' },
  { id: 'diff-viewer', label: 'Diff Viewer' },
  { id: 'heatmap', label: 'Column Heatmap' },
  { id: 'snapshot-explorer', label: 'Snapshot Explorer' },
  { id: 'sql-workbench', label: 'SQL Workbench' },
];

const activeTypes = reactive({
  ADDED: true,
  UPDATED: true,
  REMOVED: true,
});

const rangeStartId = ref('');
const rangeEndId = ref('');

const sortedSnapshots = computed(() =>
  [...snapshots.value].sort((a, b) => a.timestampMillis - b.timestampMillis),
);

const snapshotOptions = computed(() =>
  sortedSnapshots.value.map((snapshot) => ({
    id: String(snapshot.snapshotId),
    label:
      formatTimestamp(snapshot.timestampMillis) || `Snapshot ${snapshot.snapshotId}`,
  })),
);

const snapshotIndexById = computed(
  () =>
    new Map(
      sortedSnapshots.value.map((snapshot, index) => [
        String(snapshot.snapshotId),
        index,
      ]),
    ),
);

const rangeBounds = computed(() => {
  const total = sortedSnapshots.value.length;
  if (!total) return { start: 0, end: -1 };
  const startIndex = snapshotIndexById.value.get(rangeStartId.value);
  const endIndex = snapshotIndexById.value.get(rangeEndId.value);
  const normalizedStart = startIndex ?? 0;
  const normalizedEnd = endIndex ?? total - 1;
  return {
    start: Math.min(normalizedStart, normalizedEnd),
    end: Math.max(normalizedStart, normalizedEnd),
  };
});

const filteredSnapshots = computed(() => {
  if (sortedSnapshots.value.length === 0) return [];
  const { start, end } = rangeBounds.value;
  return sortedSnapshots.value.slice(start, end + 1);
});

const filteredDiffs = computed(() => {
  const allowedIds = new Set(filteredSnapshots.value.map((s) => s.snapshotId));
  return diffs.value
    .filter((diff) => allowedIds.has(diff.snapshotId))
    .map((diff) => ({
      ...diff,
      rowChanges: (diff.rowChanges ?? []).filter((change) => activeTypes[change.type]),
    }));
});

const diffsBySnapshot = computed(() =>
  Object.fromEntries(filteredDiffs.value.map((diff) => [diff.snapshotId, diff])),
);

const resetFilters = () => {
  activeTypes.ADDED = true;
  activeTypes.UPDATED = true;
  activeTypes.REMOVED = true;
  if (snapshotOptions.value.length > 0) {
    rangeStartId.value = snapshotOptions.value[0].id;
    rangeEndId.value = snapshotOptions.value[snapshotOptions.value.length - 1].id;
  }
};

const loadData = async () => {
  loading.value = true;
  error.value = null;
  try {
    // Add cache-busting headers and timestamp to prevent browser from caching responses
    const timestamp = new Date().getTime();
    const headers = {
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Pragma': 'no-cache',
      'Expires': '0'
    };

    const refreshRes = await fetch('/api/refresh', { 
      method: 'POST',
      headers 
    });
    if (!refreshRes.ok) throw new Error('Failed to refresh data source');

    // Add timestamp to URL to force cache bypass
    const snapRes = await fetch(`/api/snapshots?_t=${timestamp}`, { headers });
    if (!snapRes.ok) throw new Error('Failed to load snapshots');
    snapshots.value = await snapRes.json();

    const diffsRes = await fetch(`/api/diffs?_t=${timestamp}`, { headers });
    if (!diffsRes.ok) throw new Error('Failed to load diffs');
    diffs.value = await diffsRes.json();
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    loading.value = false;
  }
};

watch(
  sortedSnapshots,
  (list) => {
    if (list.length === 0) {
      rangeStartId.value = '';
      rangeEndId.value = '';
      return;
    }
    const ids = new Set(list.map((snapshot) => String(snapshot.snapshotId)));
    if (!ids.has(rangeStartId.value)) {
      rangeStartId.value = String(list[0].snapshotId);
    }
    if (!ids.has(rangeEndId.value)) {
      rangeEndId.value = String(list[list.length - 1].snapshotId);
    }
  },
  { immediate: true },
);

onMounted(loadData);
</script>

<template>
  <div class="app">
    <header class="header">
      <div>
        <h1>Data Changes Visualization</h1>
        <p>Explore snapshot diffs, row history, column activity, and trends.</p>
      </div>
      <button class="refresh" type="button" @click="loadData" :disabled="loading">
        Refresh
      </button>
    </header>

    <section class="filters">
      <div class="filter-group">
        <label>
          From snapshot
          <select v-model="rangeStartId">
            <option v-for="option in snapshotOptions" :key="option.id" :value="option.id">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          To snapshot
          <select v-model="rangeEndId">
            <option v-for="option in snapshotOptions" :key="option.id" :value="option.id">
              {{ option.label }}
            </option>
          </select>
        </label>
      </div>
      <div class="filter-group">
        <span class="label">Change types</span>
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
      <button class="reset" type="button" @click="resetFilters">
        Reset filters
      </button>
    </section>

    <nav class="tabs" role="tablist" aria-label="Views">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab"
        :class="{ active: activeTab === tab.id }"
        type="button"
        role="tab"
        :aria-selected="activeTab === tab.id"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </nav>

    <section class="panel" role="tabpanel">
      <p v-if="loading" class="status">Loading data...</p>
      <div v-else-if="error" class="status error">
        <p>Could not load data. {{ error }}</p>
        <button type="button" @click="loadData">Retry</button>
      </div>
      <div v-else-if="filteredSnapshots.length === 0" class="status">
        <p>No snapshots match the current filters.</p>
      </div>
      <OverviewDashboard
        v-else-if="activeTab === 'overview'"
        :snapshots="filteredSnapshots"
        :diffs="filteredDiffs"
      />
      <SnapshotTimeline
        v-else-if="activeTab === 'timeline'"
        :snapshots="filteredSnapshots"
        :diffs-by-snapshot="diffsBySnapshot"
      />
      <RowHistory
        v-else-if="activeTab === 'row-history'"
        :snapshots="filteredSnapshots"
        :diffs="filteredDiffs"
      />
      <DiffViewer
        v-else-if="activeTab === 'diff-viewer'"
        :snapshots="filteredSnapshots"
        :diffs="filteredDiffs"
      />
      <ColumnHeatmap
        v-else-if="activeTab === 'heatmap'"
        :snapshots="filteredSnapshots"
        :diffs="filteredDiffs"
      />
      <SnapshotExplorer
        v-else-if="activeTab === 'snapshot-explorer'"
        :snapshots="filteredSnapshots"
        :diffs-by-snapshot="diffsBySnapshot"
      />
      <SqlWorkbench v-else-if="activeTab === 'sql-workbench'" @refresh="loadData" />
    </section>
  </div>
</template>

<style scoped>
.app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 20px 48px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #0f172a;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0 0 6px;
  font-size: 28px;
}

.header p {
  margin: 0;
  color: #475569;
}

.refresh {
  padding: 8px 14px;
  border: 1px solid #cbd5f5;
  background: #e0e7ff;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.filter-group select {
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  min-width: 200px;
}

.filter-group .label {
  margin-right: 6px;
}

.reset {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  cursor: pointer;
  font-weight: 600;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.tab {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 600;
  color: #1e293b;
}

.tab.active {
  border-color: #4338ca;
  background: #4f46e5;
  color: #fff;
}

.panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.05);
}

.status {
  margin: 0;
  padding: 24px;
  text-align: center;
  color: #475569;
}

.status.error {
  color: #b91c1c;
}

.status button {
  margin-top: 12px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #fca5a5;
  background: #fee2e2;
  cursor: pointer;
}
</style>
