// Run with: node --test release/*.test.js
const { test } = require('node:test');
const assert = require('node:assert/strict');

const { waitForSiblingReleases } = require('./wait-for-sibling-releases');

// listRuns answers, per workflow, one entry per call: the runs of that workflow for the commit.
function scripted(answers) {
    const calls = {};
    return {
        calls,
        listRuns: async workflow => {
            calls[workflow] = (calls[workflow] || 0) + 1;
            const queue = answers[workflow];
            return queue.length > 1 ? queue.shift() : queue[0];
        },
    };
}

function clock() {
    const slept = [];
    return { slept, sleep: async seconds => { slept.push(seconds); } };
}

const quiet = { log: () => {} };
const options = { timeoutSeconds: 100, intervalSeconds: 20, graceSeconds: 40 };

test('waits for a sibling release run of the commit until it is completed', async () => {
    const runs = scripted({ Shadow: [[{ status: 'in_progress' }], [{ status: 'in_progress' }], [{ status: 'completed' }]] });
    const time = clock();

    await waitForSiblingReleases({ workflows: ['Shadow'], ...runs, ...time, ...quiet, ...options });

    assert.equal(runs.calls.Shadow, 3);
    assert.deepEqual(time.slept, [20, 20]);
});

test('counts a run waiting on a concurrency group, or queued, as in flight', async () => {
    const runs = scripted({ Shadow: [[{ status: 'pending' }], [{ status: 'waiting' }], [{ status: 'queued' }], [{ status: 'requested' }], [{ status: 'completed' }]] });
    const time = clock();

    await waitForSiblingReleases({ workflows: ['Shadow'], ...runs, ...time, ...quiet, ...options, timeoutSeconds: 1000 });

    assert.equal(runs.calls.Shadow, 5);
});

test('waits for every run of the commit, a re-run included', async () => {
    const runs = scripted({ Shadow: [[{ status: 'completed' }, { status: 'in_progress' }], [{ status: 'completed' }, { status: 'completed' }]] });
    const time = clock();

    await waitForSiblingReleases({ workflows: ['Shadow'], ...runs, ...time, ...quiet, ...options });

    assert.equal(runs.calls.Shadow, 2);
});

test('takes a sibling with no run for the commit as not triggered by it, only after the grace period', async () => {
    const runs = scripted({ Shadow: [[]] });
    const time = clock();

    await waitForSiblingReleases({ workflows: ['Shadow'], ...runs, ...time, ...quiet, ...options });

    assert.equal(runs.calls.Shadow, 3);
    assert.deepEqual(time.slept, [20, 20]);
});

test('waits for a run that appears during the grace period', async () => {
    const runs = scripted({ Shadow: [[], [{ status: 'pending' }], [{ status: 'completed' }]] });
    const time = clock();

    await waitForSiblingReleases({ workflows: ['Shadow'], ...runs, ...time, ...quiet, ...options });

    assert.equal(runs.calls.Shadow, 3);
});

test('checks every sibling, in order', async () => {
    const order = [];
    const time = clock();

    await waitForSiblingReleases({
        workflows: ['Shadow', 'Mirage', 'Doppelganger'],
        listRuns: async workflow => { order.push(workflow); return [{ status: 'completed' }]; },
        ...time, ...quiet, ...options,
    });

    assert.deepEqual(order, ['Shadow', 'Mirage', 'Doppelganger']);
});

test('fails, naming the sibling and its last status, when its run does not complete in time', async () => {
    const runs = scripted({ Shadow: [[{ status: 'pending' }]] });
    const time = clock();

    await assert.rejects(
        waitForSiblingReleases({ workflows: ['Shadow'], ...runs, ...time, ...quiet, ...options }),
        /Timed out after 100s waiting for 'Shadow'.*pending/);
});

test('fails rather than proceeds when the runs cannot be listed', async () => {
    const time = clock();

    await assert.rejects(
        waitForSiblingReleases({
            workflows: ['Shadow'],
            listRuns: async () => { throw new Error('gh: HTTP 502'); },
            ...time, ...quiet, ...options,
        }),
        /HTTP 502/);
});
