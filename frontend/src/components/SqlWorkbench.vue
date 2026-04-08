<script setup>
import { ref } from 'vue';

const sql = ref(`-- Example writes (each statement creates a snapshot)
INSERT INTO local.db.my_table VALUES (11, 'new row');
UPDATE local.db.my_table SET data = 'updated' WHERE id = 2;
DELETE FROM local.db.my_table WHERE id = 1;`);
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
</script>

<template>
  <div class="sql">
    <div class="intro">
      <strong>Interactive SQL</strong>
      <p>
        Run Spark SQL statements against <code>local.db.my_table</code>.
        Write operations (INSERT, UPDATE, DELETE) create new Iceberg snapshots
        which will be visible in the visualization after execution.
      </p>
    </div>

    <label class="editor">
      SQL editor
      <textarea v-model="sql" spellcheck="false" />
    </label>

    <div class="actions">
      <button type="button" class="primary" @click="executeSql" :disabled="running">
        {{ running ? 'Running...' : 'Execute SQL & Refresh' }}
      </button>
    </div>

    <details class="tips">
      <summary>SQL tips</summary>
      <ul>
        <li>Use <code>INSERT</code>, <code>UPDATE</code>, or <code>DELETE</code> to
          create snapshots.</li>
        <li>Run multiple statements separated by semicolons.</li>
        <li>If you create a new table, use <code>CREATE DATABASE IF NOT EXISTS local.db</code>.</li>
      </ul>
    </details>

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

.intro {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
}

.intro strong {
  display: block;
  color: #0f172a;
}

.intro p {
  margin: 6px 0 0;
  color: #475569;
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

.tips {
  font-size: 13px;
  color: #475569;
}

.tips summary {
  cursor: pointer;
  font-weight: 600;
}

.tips ul {
  margin: 8px 0 0;
  padding-left: 18px;
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
