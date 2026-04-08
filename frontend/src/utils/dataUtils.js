const isObject = (value) => value !== null && typeof value === 'object';

const toComparable = (value) => {
  if (value === undefined) return 'undefined';
  if (value === null) return 'null';
  if (isObject(value)) return JSON.stringify(value);
  return String(value);
};

export const normalizeId = (value) => {
  if (value === null || value === undefined) return null;
  return String(value);
};

export const getRowId = (change) =>
  normalizeId(change?.after?.id ?? change?.before?.id ?? null);

export const formatTimestamp = (millis) => {
  if (millis === null || millis === undefined) return '';
  return new Date(millis).toLocaleString();
};

export const formatValue = (value) => {
  if (value === null || value === undefined) return '-';
  if (isObject(value)) return JSON.stringify(value);
  return String(value);
};

export const getFieldDiffs = (change) => {
  const before = isObject(change?.before) ? change.before : {};
  const after = isObject(change?.after) ? change.after : {};
  const fields = new Set([...Object.keys(before), ...Object.keys(after)]);

  return [...fields].sort().map((field) => {
    const beforeValue = before[field];
    const afterValue = after[field];
    const changed = toComparable(beforeValue) !== toComparable(afterValue);
    return { field, before: beforeValue, after: afterValue, changed };
  });
};
