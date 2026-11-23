// Waits until every sibling detector plugin's release run of one commit has completed, so that a
// project built from those siblings - api-only-suite - resolves the versions that commit released,
// rather than the ones before.
//
// It looks for the sibling's runs of the same commit, not for the sibling's most recent run: a
// run waiting on a concurrency group is easy to miss that way, and api-only-suite 3.12.0 was
// published with shadow-api-detector 3.7.2 because of it. A run is in flight in any status but
// `completed`. A sibling with no run for the commit is taken as one the commit did not trigger,
// but only after a grace period, since its run may not have been created yet. When the runs
// cannot be listed, it fails rather than proceeds.
//
// Run from a workflow step:
//   node release/wait-for-sibling-releases.js "<workflow name>" ...
// with REPOSITORY and COMMIT_SHA in the environment, and GH_TOKEN for the gh CLI.

const { execFileSync } = require('node:child_process');

async function waitForSiblingReleases({
    workflows, listRuns, sleep, log, timeoutSeconds = 1200, intervalSeconds = 20, graceSeconds = 60,
}) {
    for (const workflow of workflows) {
        log(`Checking '${workflow}' for release runs of this commit...`);
        let elapsed = 0;
        for (;;) {
            const runs = await listRuns(workflow);
            if (runs.length === 0) {
                if (elapsed >= graceSeconds) {
                    log(`'${workflow}' has no release run for this commit: it did not release.`);
                    break;
                }
                log(`'${workflow}' has no release run for this commit yet - waiting ${intervalSeconds}s...`);
            } else {
                const inFlight = runs.filter(run => run.status !== 'completed');
                if (inFlight.length === 0) {
                    log(`'${workflow}' has finished its release run(s) for this commit.`);
                    break;
                }
                if (elapsed >= timeoutSeconds) {
                    throw new Error(`Timed out after ${timeoutSeconds}s waiting for '${workflow}' to finish `
                        + `(last status: '${inFlight[0].status}'). Re-run this workflow once it has completed.`);
                }
                log(`'${workflow}' status: '${inFlight[0].status}' - waiting ${intervalSeconds}s...`);
            }
            await sleep(intervalSeconds);
            elapsed += intervalSeconds;
        }
    }
}

function ghListRuns(repository, commit) {
    return async workflow => JSON.parse(execFileSync('gh', [
        'run', 'list', '--repo', repository, '--workflow', workflow, '--commit', commit,
        '--json', 'status,conclusion',
    ], { encoding: 'utf8' }));
}

module.exports = { waitForSiblingReleases };

if (require.main === module) {
    const { REPOSITORY, COMMIT_SHA } = process.env;
    waitForSiblingReleases({
        workflows: process.argv.slice(2),
        listRuns: ghListRuns(REPOSITORY, COMMIT_SHA),
        sleep: seconds => new Promise(resolve => setTimeout(resolve, seconds * 1000)),
        log: message => console.log(message),
    }).catch(error => {
        console.log(`::error::${error.message}`);
        process.exit(1);
    });
}
