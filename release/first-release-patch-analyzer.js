// This file lives at the repository root's release/ directory, shared across every plugin
// module's own release.config.js (each requires it via a relative "../release/..." path). A
// plain `require('@semantic-release/commit-analyzer')` would resolve relative to *this file's
// own location* (release/), which has no node_modules of its own - the real dependency is
// installed in the calling module's own node_modules (e.g. doppelganger-api-detector/node_modules),
// since that's where semantic-release's `npm ci` runs. semantic-release itself is always invoked
// with process.cwd() set to that module's directory, so resolving from there - rather than from
// this file's location - is what actually finds it.
const commitAnalyzer = require(require.resolve('@semantic-release/commit-analyzer', { paths: [process.cwd()] }));

module.exports = {
  analyzeCommits: async (pluginConfig, context) => {
    if (!context.lastRelease || !context.lastRelease.version) {
      return 'patch';
    }
    return commitAnalyzer.analyzeCommits(pluginConfig, context);
  }
};
