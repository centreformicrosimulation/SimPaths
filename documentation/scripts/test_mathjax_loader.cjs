const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const source = readFileSync(join(__dirname, '../wiki/assets/js/mathjax.js'), 'utf8');
const flush = async () => {
  for (let i = 0; i < 4; i++) await new Promise(resolve => setImmediate(resolve));
};

function harness(hasMath = false) {
  let emit;
  let root = makeRoot('initial', hasMath);
  const scripts = [], rendered = [], cleared = [], warnings = [];
  const window = {};
  function makeRoot(name, math) {
    return { name, isConnected: true, querySelector: () => math ? {} : null };
  }
  const document = {
    querySelector: () => root,
    createElement: () => ({ remove() { this.removed = true; } }),
    head: { append(script) { scripts.push(script); } }
  };
  vm.runInNewContext(source, {
    window, document, console: { warn: (...args) => warnings.push(args) },
    document$: { subscribe(callback) { emit = callback; callback(); } }
  });
  return {
    scripts, rendered, cleared, warnings,
    navigate(name, math) { root.isConnected = false; root = makeRoot(name, math); emit(); },
    anchor() { emit(); },
    load() {
      window.MathJax = {
        startup: { promise: Promise.resolve() },
        typesetPromise: async ([element]) => { rendered.push(element.name); },
        typesetClear: ([element]) => { cleared.push(element.name); }
      };
      scripts.at(-1).onload();
    }
  };
}

test('ordinary pages do not request MathJax; the first equation page loads it once', async () => {
  const h = harness();
  await flush();
  assert.equal(h.scripts.length, 0);
  h.navigate('equations', true);
  await flush();
  assert.equal(h.scripts.length, 1);
  h.load();
  await flush();
  assert.deepEqual(h.rendered, ['equations']);
  h.anchor();
  await flush();
  assert.deepEqual(h.rendered, ['equations']);
  h.navigate('more-equations', true);
  await flush();
  assert.equal(h.scripts.length, 1);
  assert.deepEqual(h.rendered, ['equations', 'more-equations']);
});

test('slow loading renders the current page and releases replaced equations', async () => {
  const h = harness(true);
  await flush();
  h.navigate('destination', true);
  h.load();
  await flush();
  assert.deepEqual(h.rendered, ['destination']);
  h.navigate('plain', false);
  await flush();
  assert.deepEqual(h.cleared, ['destination']);
});

test('a failed network request can recover on a later equation page', async () => {
  const h = harness(true);
  await flush();
  h.scripts[0].onerror();
  await flush();
  assert.equal(h.scripts[0].removed, true);
  assert.equal(h.warnings.length, 1);
  h.navigate('retry', true);
  await flush();
  assert.equal(h.scripts.length, 2);
  h.load();
  await flush();
  assert.deepEqual(h.rendered, ['retry']);
});
