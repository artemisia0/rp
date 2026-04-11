<script setup>
import { onMounted, ref } from 'vue';

const emit = defineEmits(['updated']);

const loading = ref(true);
const saving = ref(false);
const initializing = ref(false);
const error = ref(null);
const status = ref('');

const defaults = ref({
  catalog: '',
  warehouse: '',
  namespace: '',
  table: '',
});

const form = ref({
  catalog: '',
  warehouse: '',
  namespace: '',
  table: '',
});

const readError = async (res, fallback) => {
  try {
    const payload = await res.json();
    return payload?.message || payload?.error || fallback;
  } catch {
    return fallback;
  }
};

const loadConnection = async () => {
  loading.value = true;
  error.value = null;
  status.value = '';
  try {
    const res = await fetch('/api/connection');
    if (!res.ok) {
      throw new Error(await readError(res, 'Failed to load connection settings.'));
    }
    const payload = await res.json();
    defaults.value = { ...payload.defaults };
    form.value = { ...payload.current };
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    loading.value = false;
  }
};

const useDefaults = () => {
  form.value = { ...defaults.value };
  status.value = 'Default connection values applied to the form.';
  error.value = null;
};

const saveConnection = async () => {
  saving.value = true;
  error.value = null;
  status.value = '';
  try {
    const saveRes = await fetch('/api/connection', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value),
    });
    if (!saveRes.ok) {
      throw new Error(await readError(saveRes, 'Failed to save connection settings.'));
    }
    const updated = await saveRes.json();
    form.value = { ...updated };
    status.value = 'Connection updated.';
    emit('updated');
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    saving.value = false;
  }
};

const saveAndInitialize = async () => {
  initializing.value = true;
  error.value = null;
  status.value = '';
  try {
    const saveRes = await fetch('/api/connection', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value),
    });
    if (!saveRes.ok) {
      throw new Error(await readError(saveRes, 'Failed to save connection settings.'));
    }
    const updated = await saveRes.json();
    form.value = { ...updated };

    const initRes = await fetch('/api/connection/initialize', { method: 'POST' });
    if (!initRes.ok) {
      throw new Error(
        await readError(initRes, 'Failed to initialize warehouse/database/table.'),
      );
    }

    status.value = 'Connection saved and missing warehouse/database/table initialized.';
    emit('updated');
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    initializing.value = false;
  }
};

onMounted(loadConnection);
</script>

<template>
  <div class="connection">
    <div class="intro">
      <strong>Iceberg connection</strong>
      <p>
        Configure catalog name (Spark alias), warehouse URI, Iceberg database (namespace),
        and table name.
      </p>
      <p>
        Visualizations read from
        <code>{{ form.catalog || 'local' }}.{{ form.namespace || 'db' }}.{{ form.table || 'my_table' }}</code>.
      </p>
      <p class="help">
        <strong>Catalog meaning:</strong> in this app, catalog is a Spark catalog alias
        (for example <code>local</code>), while local vs remote storage is mainly determined
        by the warehouse URI. Example: <code>file:///...</code> is local filesystem;
        remote/object-store URIs like <code>s3a://...</code> or <code>hdfs://...</code>
        are remote. Plain <code>http://...</code> is not used as a Hadoop warehouse URI.
      </p>
      <p class="help">
        <strong>Initialize:</strong> creates missing warehouse directory (for local file URIs),
        namespace, and an empty table with columns <code>id INT</code>, <code>data STRING</code>.
      </p>
    </div>

    <p v-if="loading" class="status">Loading connection settings...</p>
    <div v-else class="form">
      <label>
        Catalog name (alias)
        <input v-model="form.catalog" type="text" spellcheck="false" />
      </label>

      <label>
        Warehouse URI
        <input v-model="form.warehouse" type="text" spellcheck="false" />
      </label>

      <label>
        Iceberg database (namespace)
        <input v-model="form.namespace" type="text" spellcheck="false" />
      </label>

      <label>
        Table
        <input v-model="form.table" type="text" spellcheck="false" />
      </label>

      <div class="actions">
        <button type="button" class="secondary" @click="useDefaults">
          Use defaults
        </button>
        <button
          type="button"
          class="primary"
          @click="saveConnection"
          :disabled="saving || initializing"
        >
          {{ saving ? 'Saving...' : 'Save connection' }}
        </button>
        <button
          type="button"
          class="primary"
          @click="saveAndInitialize"
          :disabled="saving || initializing"
        >
          {{ initializing ? 'Initializing...' : 'Save and initialize' }}
        </button>
      </div>
    </div>

    <p v-if="status" class="status ok">{{ status }}</p>
    <p v-if="error" class="status error">{{ error }}</p>
  </div>
</template>

<style scoped>
.connection {
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

.intro p.help {
  margin-top: 10px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-weight: 600;
  color: #1e293b;
}

input {
  max-width: 640px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
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
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
}

.status.ok {
  background: #dcfce7;
  color: #166534;
}

.status.error {
  background: #fee2e2;
  color: #b91c1c;
}
</style>
