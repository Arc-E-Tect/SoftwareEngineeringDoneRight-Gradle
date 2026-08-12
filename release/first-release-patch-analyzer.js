const commitAnalyzer = require('@semantic-release/commit-analyzer');

module.exports = {
  analyzeCommits: async (pluginConfig, context) => {
    if (!context.lastRelease || !context.lastRelease.version) {
      return 'patch';
    }
    return commitAnalyzer.analyzeCommits(pluginConfig, context);
  }
};
