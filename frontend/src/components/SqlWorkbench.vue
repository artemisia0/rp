<script setup>
import { onMounted, ref } from 'vue';

const defaultConnection = {
  catalog: 'local',
  namespace: 'db',
  table: 'my_table',
};

const defaultSqlTemplate = `-- Example writes (each statement creates a snapshot)
INSERT INTO <catalog>.<namespace>.<table> VALUES (11, 'new row');
UPDATE <catalog>.<namespace>.<table> SET data = 'updated' WHERE id = 2;
DELETE FROM <catalog>.<namespace>.<table> WHERE id = 1;`;

const toSqlTemplate = (connection) =>
  defaultSqlTemplate
    .replaceAll('<catalog>', connection.catalog)
    .replaceAll('<namespace>', connection.namespace)
    .replaceAll('<table>', connection.table);

const sql = ref(toSqlTemplate(defaultConnection));
const running = ref(false);
const result = ref(null);
const error = ref(null);

const executeSql = async () => {
  if (!sql.value.trim()) {
    error.value = 'SQL is empty.';
    return;
  }
  running.value = true;
  error.value = null;
  result.value = null;
  try {
    const res = await fetch('/api/sql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sql: sql.value }),
    });
    const data = await res.json();
    if (!res.ok) {
      error.value = data.message || data.error || 'SQL execution failed.';
      result.value = null;
      return;
    }
    result.value = data;
    if (!data.success) {
      error.value = data.stderr || data.stdout || 'SQL execution failed.';
    }
    // Full page reload to show new snapshots
    window.location.reload();
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    running.value = false;
  }
};

onMounted(async () => {
  try {
    const res = await fetch('/api/connection');
    if (!res.ok) return;
    const payload = await res.json();
    const current = payload?.current;
    if (!current?.catalog || !current?.namespace || !current?.table) return;
    sql.value = toSqlTemplate(current);
  } catch {
    // Keep fallback defaults when connection cannot be loaded.
  }
});
</script>

<template>
  <div class="sql">
    <label class="editor">
      SQL editor
      <textarea v-model="sql" spellcheck="false" />
    </label>

    <div class="actions">
      <button type="button" class="primary" @click="executeSql" :disabled="running">
        {{ running ? 'Running...' : 'Execute SQL & Refresh' }}
      </button>
    </div>

    <div v-if="running" class="status">Running SQL...</div>
    <div v-else-if="error && !result" class="status error">{{ error }}</div>
    <div v-else-if="result" class="output">
      <div class="meta">
        <span>Status: {{ result.success ? 'Success' : 'Failed' }}</span>
        <span>Exit code: {{ result.exitCode }}</span>
      </div>
      <pre>{{ result.stdout || 'No output.' }}</pre>
    </div>
    <div v-if="error && result" class="status error">{{ error }}</div>
  </div>
</template>

<style scoped>
.sql {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-weight: 600;
  color: #1e293b;
}

textarea {
  min-height: 180px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

button {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  cursor: pointer;
  font-weight: 600;
}

button.primary {
  background: #4f46e5;
  border-color: #4338ca;
  color: #ffffff;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status {
  padding: 12px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
  font-size: 14px;
}

.status.error {
  background: #fee2e2;
  color: #b91c1c;
}

.output {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px;
  font-size: 12px;
}

.meta {
  display: flex;
  gap: 16px;
  color: #94a3b8;
  margin-bottom: 8px;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
